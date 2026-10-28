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

# Políticas administradas para `/api/*` (lección 6): sin caché (las respuestas de la
# API son dinámicas) y reenviando al origen todo lo del visitante salvo el `Host`
# (API Gateway rechaza peticiones cuyo Host no sea el suyo).
data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" {
  name = "Managed-AllViewerExceptHostHeader"
}

# SPA (Angular): al recargar /dashboard, S3 no tiene ese archivo. Esta función
# reescribe las rutas SIN extensión a /index.html y Angular resuelve la ruta en el
# navegador. Sustituye al `custom_error_response` de la lección 2, que era global:
# con la API detrás de la misma distribución habría convertido los 403/404 de la API
# en un index.html con código 200.
resource "aws_cloudfront_function" "spa_rewrite" {
  name    = "${local.name_prefix}-spa-rewrite"
  runtime = "cloudfront-js-2.0"
  comment = "Rutas sin extension -> /index.html (SPA)"
  publish = true

  code = <<-EOT
    function handler(event) {
      var request = event.request;
      if (request.uri.indexOf('.') === -1) {
        request.uri = '/index.html';
      }
      return request;
    }
  EOT
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

  # Segundo origen (lección 6): API Gateway. Su dominio es la URL `execute-api` sin
  # el `https://`. CloudFront solo habla HTTPS con él.
  origin {
    origin_id   = "api-gateway"
    domain_name = replace(aws_apigatewayv2_api.api.api_endpoint, "https://", "")

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # Las rutas `/api/*` van a la API; todo lo demás cae en el comportamiento por defecto (S3).
  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "api-gateway"
    viewer_protocol_policy = "https-only"
    allowed_methods        = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer_except_host.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-front"
    viewer_protocol_policy = "redirect-to-https" # el navegador siempre usa HTTPS
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_optimized.id
    response_headers_policy_id = data.aws_cloudfront_response_headers_policy.security_headers.id

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.spa_rewrite.arn
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
