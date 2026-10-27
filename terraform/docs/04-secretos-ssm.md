# Lección 4: configuración y secretos en SSM Parameter Store

La API Spring Boot lee su configuración de variables de entorno. Aquí creas dónde viven en AWS; en la
lección 5 la tarea de ECS las recibirá. El código está en [`../envs/qa/ssm.tf`](../envs/qa/ssm.tf) y el
cargador en [`../scripts/load-ssm-from-env.mjs`](../scripts/load-ssm-from-env.mjs).

```
Terraform ──crea (relleno)──► /aglacita-tf/qa/JWT_SECRET = "CHANGE_ME"
script .mjs ──sobrescribe──►  /aglacita-tf/qa/JWT_SECRET = <valor real>   (Terraform nunca lo ve)
```

## El problema: los secretos acaban en el state

Un `aws_ssm_parameter` normal usa `value = ...`. Terraform guarda ese valor **en texto plano en
`terraform.tfstate`**, y además lo vuelve a leer de AWS en cada `plan`. `sensitive = true` solo lo oculta en
pantalla. Si el state se filtra (o se sube a un bucket mal protegido), se filtran todas tus claves.

Solución en dos partes:

1. **`value_wo` (write-only):** Terraform envía el valor a AWS pero no lo guarda en el state ni en el plan.
   Lo usamos solo para el relleno `CHANGE_ME`, de modo que el parámetro exista.
2. **El valor real llega por fuera:** un script lo escribe con la AWS CLI. Terraform no lo conoce, no lo lee
   y no detecta "cambios".

> Corrección importante: la primera idea era `value` + `lifecycle { ignore_changes = [value] }`. Evita el
> diff, pero **no** evita que el provider lea el valor real hacia el state. Por eso se usa `value_wo`
> (requiere Terraform >= 1.11; el proyecto lo exige en `versions.tf`).

## Conceptos de Terraform

### Argumentos de solo escritura

```hcl
value_wo         = local.ssm_placeholder
value_wo_version = 1
```

Un atributo `_wo` nunca se persiste. Como Terraform no puede comparar algo que no guarda, cambiarlo no
dispara una actualización: para forzar el reenvío se sube `value_wo_version`.

### `for_each` con conjunto y con mapa

```hcl
for_each = local.secret_parameters   # toset([...]): each.key es el nombre
for_each = local.plain_parameters    # map: each.key es el nombre, each.value el valor
```

Un conjunto sirve cuando solo importa el nombre; un mapa, cuando cada elemento trae un dato propio.

### Encadenar con la lección 2

```hcl
HOST_CORS = "https://${aws_cloudfront_distribution.front.domain_name}"
```

CORS se calcula a partir de la distribución. Si recreas CloudFront y cambia el dominio, el parámetro se
actualiza solo; en el QA manual hay que acordarse de editarlo.

## Qué parámetros se crean

La ruta es `/aglacita-tf/qa/<NOMBRE>`, y el nombre coincide con la variable de `application.yaml`.

| Parámetro | Tipo | Valor |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | SecureString | del bloque comentado `# Base de datos qa` del `.env` (las `DB_*` activas apuntan a `localhost`); si falta, el script las pide |
| `JWT_SECRET` | SecureString | **nuevo**, generado por el script (no se reutiliza el de desarrollo) |
| `DEEPSEEK_API_KEY`, `API_OPENAI_KEY` | SecureString | copiados de `backend/.env` |
| `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` | SecureString | copiados de `backend/.env` |
| `SPRING_PROFILES_ACTIVE` | String | `qa` |
| `HOST_CORS` | String | `https://<dominio de CloudFront>` |

Son las variables **sin valor por defecto** del backend: si falta una, la app no arranca. Las opcionales
(`SYSTEM_USER_EMPLOYEE_META`, `VAPID_*`, `OTP_*`) quedan fuera; para sumarlas, agrégalas a
`secret_parameters` y a `COPY_FROM_ENV` en el script. No pongas un `CHANGE_ME` en una opcional: la app lo
leería como valor real y fallaría.

Costo: los parámetros estándar son gratis y SecureString usa la clave gestionada `aws/ssm`, también gratis.

## Permisos de IAM necesarios

Crea la política desde **IAM → Políticas → Crear política** (no como política en línea, que tiene un tope
de 2048 caracteres) con el nombre `terraform-aglacita-tf-ssm`, y adjúntala a `angel`:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "TerraformSsmParameters",
			"Effect": "Allow",
			"Action": [
				"ssm:PutParameter",
				"ssm:GetParameter",
				"ssm:GetParameters",
				"ssm:DeleteParameter",
				"ssm:AddTagsToResource",
				"ssm:RemoveTagsFromResource",
				"ssm:ListTagsForResource"
			],
			"Resource": "arn:aws:ssm:*:*:parameter/aglacita-tf/*"
		},
		{
			"Sid": "TerraformSsmDescribe",
			"Effect": "Allow",
			"Action": "ssm:DescribeParameters",
			"Resource": "*"
		}
	]
}
```

Aquí sí se puede acotar `Resource`: el nombre de un parámetro se conoce de antemano, así que `angel` solo
toca `/aglacita-tf/*` y nunca los del QA manual. `DescribeParameters` no admite restricción por recurso.

## Ejecutar

```bash
cd terraform/envs/qa
terraform plan
```

Esperado: `Plan: 10 to add, 0 to change, 0 to destroy.` (8 SecureString y 2 String).

```bash
terraform apply     # escribe yes
```

Los parámetros existen, pero los 8 secretos valen `CHANGE_ME`. **La app no debe usarlos todavía.**

### Cargar los valores reales

```bash
cd terraform
node scripts/load-ssm-from-env.mjs            # simulacro: lista qué haría, sin tocar AWS
node scripts/load-ssm-from-env.mjs --apply    # escribe en AWS
```

El `.env` tiene dos tipos de líneas: las activas (desarrollo) y las comentadas bajo un encabezado que
menciona `qa` (`# Base de datos qa` seguido de `# DB_URL=...`). El script usa las activas para las claves
de servicios externos y el bloque `qa` para la base de datos. Si una `DB_*` no está en ese bloque, te la
pide en la terminal sin mostrarla. Rechaza un `DB_URL` que no sea `jdbc:` o que apunte a `localhost`.

El script nunca imprime un valor, escribe cada uno en un archivo temporal que borra al terminar (así no
aparece en la lista de procesos) y omite los parámetros que ya tienen valor real, para no regenerar el
`JWT_SECRET` por accidente (invalidaría las sesiones). `--force` los reescribe.

## Qué observar

1. `terraform state show 'aws_ssm_parameter.secret["JWT_SECRET"]'`: no aparece ningún `value`. Con `value`
   normal verías el secreto completo; esa es la diferencia.
2. Después de cargar, `terraform plan` sigue diciendo `No changes`: Terraform no sabe del valor real.
3. En la consola, Systems Manager → Parameter Store: el tipo es `SecureString` y la versión sube a 2 tras
   cargar. El script se apoya en eso: versión 1 = relleno.
4. `grep -c CHANGE_ME terraform.tfstate`: el relleno sí aparece, el secreto real no.

## Limpiar

```bash
terraform destroy
```

Borra los 10 parámetros, valores reales incluidos. Si quieres conservarlos, no destruyas esta lección.

## Ejercicios

1. Agrega `VAPID_PRIVATE_KEY` a `secret_parameters` y `COPY_FROM_ENV`. ¿Cuántos recursos nuevos muestra el
   `plan`? (Pista: uno, por el `for_each`.)
2. Cambia `value_wo` a `value` en un parámetro de prueba y mira `terraform state show`. ¿Qué ves? Revierte.
3. Pregunta: ¿por qué `HOST_CORS` es un `String` gestionado por Terraform y `JWT_SECRET` no?

Siguiente: Lección 5, ECS Fargate + Cloud Map + logs.
