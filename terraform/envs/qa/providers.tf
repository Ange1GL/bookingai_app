provider "aws" {
  region = var.aws_region

  # Se aplican a TODOS los recursos que soporten tags: te sirve para filtrar costos
  # y para saber, desde la consola, qué fue creado por Terraform.
  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
