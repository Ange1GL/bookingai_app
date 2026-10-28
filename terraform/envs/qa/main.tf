# ---------------------------------------------------------------------------
# Lección 1: bucket S3 privado para el front-end (Angular)
# ---------------------------------------------------------------------------

# `data` LEE algo que ya existe; no crea nada. Aquí: ¿en qué cuenta estoy?
data "aws_caller_identity" "current" {}

# `locals` son "variables internas" calculadas. Los nombres de bucket S3 son únicos
# en TODO el mundo, por eso añadimos el id de cuenta.
locals {
  name_prefix       = "${var.project}-${var.environment}"
  front_bucket_name = "${local.name_prefix}-front-${data.aws_caller_identity.current.account_id}"

  # Valor de relleno de los secretos de SSM. El script de carga reconoce este texto
  # y la lección 5 puede detectar parámetros que aún no se han cargado.
  ssm_placeholder = "CHANGE_ME"
}

# `resource "<tipo>" "<nombre local>"`: el tipo viene del provider (aws_s3_bucket);
# el nombre local solo sirve para referenciarlo desde otros recursos.
resource "aws_s3_bucket" "front" {
  bucket        = local.front_bucket_name
  force_destroy = var.front_force_destroy
}

# Bloqueo de acceso público: aunque alguien escriba por error una política pública
# o un ACL público, S3 lo rechaza. Es la defensa en profundidad más barata que existe.
# Fíjate en `aws_s3_bucket.front.id`: referenciar un atributo crea una DEPENDENCIA
# implícita. Terraform sabe que primero va el bucket y luego esto.
resource "aws_s3_bucket_public_access_block" "front" {
  bucket = aws_s3_bucket.front.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Cifrado en reposo con SSE-S3 (gratis). S3 ya lo hace por defecto desde 2023;
# declararlo hace explícita la intención y evita que alguien lo cambie sin que se note.
resource "aws_s3_bucket_server_side_encryption_configuration" "front" {
  bucket = aws_s3_bucket.front.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Versionado: permite recuperar un despliegue anterior si subes algo roto.
resource "aws_s3_bucket_versioning" "front" {
  bucket = aws_s3_bucket.front.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Con versionado, los archivos viejos se acumulan. Esta regla borra versiones
# no vigentes tras 30 días. (Corrige el "versionado deshabilitado" de tu QA manual
# sin pagar por almacenamiento infinito.)
resource "aws_s3_bucket_lifecycle_configuration" "front" {
  bucket = aws_s3_bucket.front.id

  # El versionado debe existir antes que la regla; no hay atributo que lo implique,
  # así que lo declaramos a mano.
  depends_on = [aws_s3_bucket_versioning.front]

  rule {
    id     = "expire-old-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
