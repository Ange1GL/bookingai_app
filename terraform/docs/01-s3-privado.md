# Lección 1: bucket S3 privado

Primer bloque de tu arquitectura: donde vive el build de Angular. El código está en
[`../envs/qa/main.tf`](../envs/qa/main.tf).

## Conceptos de Terraform

### `resource`

```hcl
resource "aws_s3_bucket" "front" { ... }
#         └─ tipo (provider) └─ nombre local
```

El tipo dice **qué** crear; el nombre local es solo tu etiqueta para referenciarlo. La dirección
completa en el state es `aws_s3_bucket.front`.

### Referencias y dependencias implícitas

```hcl
bucket = aws_s3_bucket.front.id
```

Al usar un atributo de otro recurso, Terraform deduce el orden (primero el bucket, luego esto) y
construye un **grafo de dependencias**. Los recursos sin dependencia entre sí se crean **en paralelo**.
Cuando no hay referencia pero sí orden (como el versionado antes del ciclo de vida), se usa
`depends_on` explícito.

### `variable`, `local`, `output`

- `variable`: entrada desde fuera (`variables.tf`, `terraform.tfvars`, `-var`). Puede tener `type`,
  `default` y `validation`; probamos la validación del nombre del proyecto.
- `local`: valor calculado interno (`local.front_bucket_name`). Evita repetir expresiones.
- `output`: valor de salida, visible tras `apply` y consumible por otros módulos.

### `data`

```hcl
data "aws_caller_identity" "current" {}
```

Un `data` **lee** algo que ya existe (aquí tu id de cuenta) y no lo crea ni lo gestiona. Lo usamos
para que el nombre del bucket sea único globalmente sin escribir tu cuenta a mano.

### `default_tags`

En `providers.tf`: todos los recursos reciben `Project`, `Environment` y `ManagedBy=terraform`.
En AWS Cost Explorer puedes filtrar por esas etiquetas.

## Por qué cinco recursos para un bucket

En el provider AWS v4+, cada aspecto del bucket es un recurso aparte. Es verboso, pero hace cada
decisión de seguridad visible en el `plan`.

| Recurso | Medida | Qué previene |
|---|---|---|
| `aws_s3_bucket` | El bucket | n/a |
| `..._public_access_block` | Bloquea ACL y políticas públicas | Un error de configuración que exponga tus archivos |
| `..._server_side_encryption_configuration` | Cifrado en reposo SSE-S3 | Lectura de discos físicos; hace explícita la intención |
| `..._versioning` | Guarda versiones previas | Despliegue roto sin vuelta atrás |
| `..._lifecycle_configuration` | Borra versiones viejas a los 30 días | Que el versionado cueste cada vez más |

Mejora respecto al QA manual: allí el versionado estaba deshabilitado.

**Defensa en profundidad:** el bucket sigue sin ser legible por nadie. En la lección 2 CloudFront
será el único con permiso (OAC); cada capa se suma a la anterior.

## `force_destroy`

Un bucket con archivos no se puede borrar. `front_force_destroy = true` (solo en tu
`terraform.tfvars` de QA) permite que `terraform destroy` lo vacíe. En producción déjalo en `false`
(valor por defecto): es una protección contra borrar datos por accidente.

## Ejecutar

```bash
cd terraform/envs/qa
cp terraform.tfvars.example terraform.tfvars   # una sola vez
terraform init
terraform plan
```

El plan esperado termina con:

```
Plan: 5 to add, 0 to change, 0 to destroy.
```

Antes de aplicar, léelo: ¿ves los 5 recursos? ¿Algún `-`? Luego:

```bash
terraform apply        # escribe yes
```

## Qué observar tras el `apply`

1. **Outputs**: `account_id`, `front_bucket_name`, `front_bucket_arn`.
2. **Consola de AWS > S3**: bucket `aglacita-tf-qa-front-<cuenta>`, con las etiquetas de `default_tags`
   y el bloqueo de acceso público activo.
3. **`terraform state list`**: los 5 recursos que Terraform conoce.
4. **`terraform show`**: el state en formato legible.
5. **Idempotencia**: ejecuta `terraform apply` otra vez. Debe decir `No changes`.
6. **Deriva (drift)**: en la consola, desactiva el versionado a mano. Ejecuta `terraform plan`: verá
   la diferencia y propondrá corregirla. El código es la fuente de verdad.

## Limpiar

```bash
terraform destroy      # escribe yes
```

Un bucket vacío no cuesta nada, así que puedes dejarlo mientras avanzas a la lección 2.

## Ejercicios

1. Cambia `noncurrent_days` de 30 a 14 y mira el `plan`: aparece `~` (modificación in situ), no `-/+`.
2. Cambia `var.project` a `Aglacita` (con mayúscula): la `validation` falla antes de tocar AWS.
3. Pregunta: ¿por qué no hay ningún `aws_s3_bucket_policy` aún? (Respuesta: nadie debe leer el bucket
   todavía; en la lección 2 solo CloudFront podrá.)

Siguiente: Lección 2, CloudFront con OAC.
