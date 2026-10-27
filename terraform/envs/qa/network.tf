# ---------------------------------------------------------------------------
# Lección 3: red. No creamos VPC: usamos la VPC por defecto de la cuenta, igual
# que el QA manual (subredes públicas, sin NAT Gateway, que costaría más que todo
# el cómputo de QA).
# ---------------------------------------------------------------------------

# `default = true` filtra la VPC por defecto de la región. Es un `data`: solo LEE,
# jamás la crea ni la borra, así que `terraform destroy` no puede dañarla.
data "aws_vpc" "default" {
  default = true
}

# Todas las subredes de esa VPC. Las usarán el servicio ECS (lección 5) y el
# VPC Link (lección 6). `filter` equivale a `--filters` de la AWS CLI.
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}
