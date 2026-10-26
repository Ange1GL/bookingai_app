# ---------------------------------------------------------------------------
# Lección 2: CloudFront delante del bucket privado (OAC)
#
# Terraform lee TODOS los .tf de la carpeta como uno solo: por eso podemos poner
# esta lección en un archivo nuevo sin tocar main.tf (la S3 de la lección 1).
# ---------------------------------------------------------------------------

# Origin Access Control (OAC): la "identidad" con la que CloudFront firma sus
# peticiones a S3. Sustituye al antiguo OAI. Es lo que permite que el bucket siga
# 100% privado y aun así CloudFront pueda leerlo.
resource "aws_cloudfront_origin_access_control" "front" {
  name                              = "${local.name_prefix}-front-oac"
  description                       = "Permite a CloudFront leer el bucket privado del front"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# `data` que busca políticas ADMINISTRADAS por AWS (ya existen, no las creamos).
# Buscarlas por nombre evita copiar ids opacos como "658327ea-f89d-4fab-...".
data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

# Añade cabeceras de seguridad (HSTS, X-Content-Type-Options, X-Frame-Options,
# Referrer-Policy...) a cada respuesta. Mejora "conveniente" de tu documento.
data "aws_cloudfront_response_headers_policy" "security_headers" {
  name = "Managed-SecurityHeadersPolicy"
}

resource "aws_cloudfront_distribution" "front" {
  enabled             = true
  comment             = "${local.name_prefix} front (Angular)"
  default_root_object = "index.html"
  price_class         = "PriceClass_100" # solo EE. UU., Canadá y Europa: el más barato

  origin {
    origin_id                = "s3-front"
    domain_name              = aws_s3_bucket.front.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.front.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-front"
    viewer_protocol_policy = "redirect-to-https" # el navegador siempre usa HTTPS
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_optimized.id
    response_headers_policy_id = data.aws_cloudfront_response_headers_policy.security_headers.id
  }

  # SPA (Angular): al recargar /dashboard, S3 no tiene ese archivo y responde 403.
  # Devolvemos index.html con código 200 y Angular resuelve la ruta en el navegador.
  # Corrige el punto "necesario" de tu documento.
  dynamic "custom_error_response" {
    for_each = toset([403, 404])

    content {
      error_code            = custom_error_response.value
      response_code         = 200
      response_page_path    = "/index.html"
      error_caching_min_ttl = 10
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # Certificado *.cloudfront.net incluido. Dominio propio + ACM llegará en producción.
  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

# Política del bucket: SOLO el servicio CloudFront, y SOLO esta distribución,
# puede leer objetos. Sin esto el bucket privado no sería legible por nadie.
data "aws_iam_policy_document" "front_bucket" {
  statement {
    sid       = "AllowCloudFrontRead"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.front.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.front.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "front" {
  bucket = aws_s3_bucket.front.id
  policy = data.aws_iam_policy_document.front_bucket.json

  # S3 rechaza cambios simultáneos de política y de bloqueo público; forzamos el orden.
  depends_on = [aws_s3_bucket_public_access_block.front]
}

# Página de prueba para ver todo funcionando sin desplegar Angular todavía.
# `filemd5` hace que Terraform vuelva a subirla cuando el archivo cambie.
resource "aws_s3_object" "index" {
  bucket       = aws_s3_bucket.front.id
  key          = "index.html"
  source       = "${path.module}/site/index.html"
  content_type = "text/html; charset=utf-8"
  etag         = filemd5("${path.module}/site/index.html")
}
