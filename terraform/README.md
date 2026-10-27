# Terraform: arquitectura QA de Aglacita (tutorial)

Levanta con Terraform la misma arquitectura QA que se montó a mano en AWS (CloudFront + S3 privado +
API Gateway HTTP + VPC Link + Cloud Map + ECS Fargate + SSM), aprendiendo Terraform por el camino.
El entorno se crea con prefijo `aglacita-tf` para no chocar con el QA manual (`aglacita-qa`).

Plan completo: [`../.plans/terraform-aglacita-qa.md`](../.plans/terraform-aglacita-qa.md).

## Requisitos

- Terraform >= 1.11 (`terraform version`)
- Node.js (para `scripts/load-ssm-from-env.mjs`, que carga los secretos en SSM)
- AWS CLI v2 con credenciales válidas (`aws sts get-caller-identity`)
- Una cuenta de AWS; el entorno completo cuesta ~40 USD/mes encendido (consulta `destroy` al terminar)

## Uso

```bash
cd terraform/envs/qa
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
terraform destroy
```

## Lecciones

| # | Lección | Estado |
|---|---|---|
| 0 | [Preparación y ciclo de Terraform](docs/00-preparacion.md) | Lista |
| 1 | [S3 privado](docs/01-s3-privado.md) | Lista |
| 2 | [CloudFront + OAC](docs/02-cloudfront-oac.md) | Lista |
| 3 | [Red y security groups](docs/03-red-security-groups.md) | Lista |
| 4 | [Secretos en SSM](docs/04-secretos-ssm.md) | Lista |
| 5 | ECS Fargate + Cloud Map + logs | Pendiente |
| 6 | API Gateway + VPC Link | Pendiente |
| 7 | Módulos, remote state e import | Pendiente |

## Seguridad

`*.tfstate` y `*.tfvars` están en `.gitignore`: nunca los subas. Ningún secreto va en archivos `.tf`.
