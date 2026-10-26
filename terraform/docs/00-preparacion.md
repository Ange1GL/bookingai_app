# Lección 0: preparación y el ciclo de Terraform

## Qué es Terraform

Escribes en archivos `.tf` **cómo quieres que quede** tu infraestructura (declarativo). Terraform
compara tres cosas y calcula la diferencia:

```
 tu código (.tf)  ──┐
                    ├─►  terraform plan  ─►  "para llegar a lo deseado: +3 crear, ~1 cambiar, -0 borrar"
 el state (.tfstate)┤
 AWS real (API)   ──┘
```

- **Código**: lo deseado.
- **State**: la libreta donde Terraform apunta qué recursos creó y sus ids. Sin ella no sabría que el
  bucket `X` es el recurso `aws_s3_bucket.front` de tu código.
- **AWS real**: Terraform lo consulta en cada `plan` (se llama *refresh*).

### Idempotencia (lo que preguntabas)

Si ejecutas `apply` y luego otra vez `apply` sin tocar el código, la segunda vez dice
`No changes. Your infrastructure matches the configuration.` No crea un segundo bucket: ve que ya
existe en el state y que coincide. Por eso puedes repetirlo sin miedo, y por eso "levantar todo con un
comando" es seguro: el comando significa "haz que la realidad coincida con esto", no "crea esto".

## Los 5 comandos

| Comando | Qué hace | ¿Toca AWS? |
|---|---|---|
| `terraform init` | Descarga el provider de AWS y prepara la carpeta `.terraform/` | No |
| `terraform validate` | Revisa sintaxis y tipos | No |
| `terraform fmt` | Formatea el código con el estilo estándar | No |
| `terraform plan` | Muestra qué cambiaría (`+` crea, `~` modifica, `-` borra, `-/+` reemplaza) | Solo lectura |
| `terraform apply` | Ejecuta el plan, previa confirmación escribiendo `yes` | **Sí** |
| `terraform destroy` | Borra todo lo que está en el state | **Sí** |

Regla de oro: **lee el `plan` completo antes de escribir `yes`**. Fíjate especialmente en `-` y `-/+`
(reemplazo = borrar y recrear, puede significar pérdida de datos).

## Anatomía de un proyecto

Terraform lee **todos los `.tf` de la carpeta** como si fueran uno solo. Separarlos es solo orden:

| Archivo | Para qué |
|---|---|
| `versions.tf` | Versión de Terraform y de los providers (`required_providers`) |
| `providers.tf` | Configuración del provider (región, tags por defecto) |
| `variables.tf` | Entradas parametrizables |
| `main.tf` | Los recursos |
| `outputs.tf` | Valores que se imprimen o que consumen otros módulos |
| `terraform.tfvars` | Valores reales de las variables (no se sube a git) |
| `.terraform.lock.hcl` | Versiones exactas de providers; **sí se sube a git** |

### Versiones: `~> 6.0`

`version = "~> 6.0"` significa "6.x pero nunca 7.0". Los cambios mayores de un provider pueden romper
tu código; así decides tú cuándo actualizar. El lock file congela además la versión exacta (por ejemplo
6.4.1) para que tu compañero y tu CI usen la misma.

## Seguridad: credenciales

El provider de AWS busca credenciales en este orden: variables de entorno, perfil de
`~/.aws/credentials`, SSO, etc. **Nunca escribas claves en un `.tf`.**

Tu `aws sts get-caller-identity` responde con un usuario IAM (`UserId` que empieza por `AIDA`), es decir,
claves de larga duración guardadas en disco. Funciona para aprender, pero la práctica recomendada es:

1. Para tu equipo local: **IAM Identity Center (SSO)** con `aws configure sso`, que da credenciales
   temporales que caducan solas.
2. Para CI (GitHub Actions): **OIDC**, un rol que GitHub asume sin guardar ninguna clave.
3. Que el usuario/rol de Terraform tenga solo los permisos necesarios, no `AdministratorAccess`
   (para QA de aprendizaje, admin es aceptable; para producción, no).

## El `.gitignore` de esta carpeta

Está en [`../.gitignore`](../.gitignore). Lo importante: `*.tfstate` y `*.tfvars` **jamás** se suben. El
state guarda en texto plano los valores de atributos de los recursos (incluidas contraseñas si las
creas con Terraform). En la lección 7 lo moveremos a un bucket S3 cifrado.

## Ejercicio

```bash
cd terraform/envs/qa
terraform init
terraform validate
```

Observa qué crea `init`: la carpeta `.terraform/` (provider descargado) y `.terraform.lock.hcl`.
Abre el lock file y mira la versión exacta elegida.

Siguiente: [Lección 1, S3 privado](01-s3-privado.md).
