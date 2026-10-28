terraform {
  # Versión mínima de Terraform que entiende este código.
  # 1.11 es la primera que admite argumentos de solo escritura (`value_wo`).
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0" # acepta 6.x, nunca 7.0: evita sorpresas por cambios mayores
    }
  }
}
