# CLAUDE.md (terraform/)

## Propósito
Infraestructura como código de la arquitectura AWS QA de Aglacita, escrita como tutorial de Terraform.
Cada lección en `docs/NN-*.md` corresponde a un commit; el código crece de forma incremental.

## Stack
Terraform >= 1.9, provider `hashicorp/aws ~> 6.0`, región `us-east-1`.

## Estructura
- `envs/qa/`: root module que se ejecuta (`versions.tf`, `providers.tf`, `variables.tf`, `main.tf`, `outputs.tf`).
  Cada bloque de la arquitectura vive en su propio archivo: `main.tf` (S3), `cloudfront.tf` (CloudFront + OAC),
  `network.tf` (VPC default y subredes como `data`), `security_groups.tf` (`vpclink-sg`, `api-sg`).
- `envs/qa/site/`: página de prueba que se sube al bucket (hasta que se despliegue el build de Angular).
- `modules/`: módulos reutilizables (se introducen en la lección 7).
- `docs/`: una lección por archivo (concepto, código, comandos, qué observar).

## Comandos
Desde `envs/qa/`: `terraform fmt`, `init`, `validate`, `plan`, `apply`, `destroy`.

## Convenciones
- Prefijo de recursos `aglacita-tf-qa-*` (el QA manual usa `aglacita-qa-*`; no mezclar).
- Nunca ejecutar `apply` o `destroy` sin confirmación explícita del usuario; leer el `plan` juntos.
- Sin secretos en `.tf`; `*.tfstate` y `*.tfvars` fuera de git; `.terraform.lock.hcl` sí se versiona.
- La VPC default es compartida con el QA manual: se lee con `data`, nunca se modifica el SG `default` salvo con `harden_default_sg`.
- Las reglas de security group son recursos `aws_vpc_security_group_*_rule` (uno por regla), no bloques inline.
- Los recursos S3 se declaran como recursos separados (patrón del provider v4+).
- Scripts auxiliares en Node `.mjs`, no shell.
- Comentarios de los `.tf` en español y explican el porqué, por ser material didáctico.
