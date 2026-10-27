# ---------------------------------------------------------------------------
# Lección 5 (1/4): repositorio ECR donde vive la imagen de la API.
#
# Terraform crea el repositorio VACÍO. La imagen la construyes y subes tú
# (docs/05-ecs-fargate.md); Terraform solo la referencia por nombre y etiqueta.
# ---------------------------------------------------------------------------

resource "aws_ecr_repository" "api" {
  name = "${local.name_prefix}-api"

  # Con etiquetas inmutables, `abc1234` apunta SIEMPRE a la misma imagen. Con
  # `latest` mutable, el mismo `terraform apply` podría desplegar código distinto
  # sin que el plan lo note.
  image_tag_mutability = "IMMUTABLE"

  # Permite `terraform destroy` aunque haya imágenes. Solo para QA.
  force_delete = var.ecr_force_delete

  image_scanning_configuration {
    scan_on_push = true # busca CVE conocidos en cada imagen subida (gratis, escaneo básico)
  }

  encryption_configuration {
    encryption_type = "AES256" # SSE gestionado por ECR, sin costo
  }
}

# Sin esta regla las imágenes se acumulan y se pagan por GB-mes. Se conservan las
# últimas N; las anteriores caducan solas.
resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name

  # `jsonencode` convierte un objeto de HCL a JSON: no hay que escapar comillas a mano.
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conserva solo las ultimas ${var.ecr_keep_images} imagenes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.ecr_keep_images
      }
      action = { type = "expire" }
    }]
  })
}
