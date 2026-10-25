terraform {
  # Versión mínima de Terraform que entiende este código.
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0" # acepta 6.x, nunca 7.0: evita sorpresas por cambios mayores
    }
  }
}
