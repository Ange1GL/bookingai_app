# ---------------------------------------------------------------------------
# Lección 6: API Gateway (HTTP API) + VPC Link hacia la API en Fargate.
#
#   CloudFront (/api/*) ─► API Gateway ─► VPC Link (vpclink-sg) ─► Cloud Map ─► tarea (api-sg)
#
# API Gateway vive FUERA de tu VPC; el VPC Link es el túnel que le permite llegar
# a la tarea privada. Cloud Map le dice qué IP y puerto tiene la tarea en cada
# momento, así que no hace falta un balanceador (ALB) con cuota fija por hora.
# ---------------------------------------------------------------------------

# HTTP API (no REST API): más simple y barata, se cobra por petición. No lleva CORS
# porque el navegador llama al mismo origen (CloudFront reparte `/` y `/api/*`).
resource "aws_apigatewayv2_api" "api" {
  name          = "${local.name_prefix}-api"
  description   = "HTTP API de ${local.name_prefix}, detrás de CloudFront"
  protocol_type = "HTTP"
}

# API Gateway no ofrece VPC Links en todas las zonas de disponibilidad. La VPC default
# tiene una subred por zona, así que se leen una a una y se descartan las zonas no
# soportadas (por id de zona, que es estable; el nombre "us-east-1e" varía por cuenta).
data "aws_subnet" "default" {
  for_each = toset(data.aws_subnets.default.ids)

  id = each.value
}

locals {
  vpc_link_subnet_ids = [
    for subnet in data.aws_subnet.default : subnet.id
    if !contains(var.vpc_link_unsupported_az_ids, subnet.availability_zone_id)
  ]
}

# El túnel hacia la VPC. Crea interfaces de red en estas subredes, protegidas por
# `vpclink-sg` (lección 3), que solo puede salir hacia `api-sg` por el puerto de la app.
resource "aws_apigatewayv2_vpc_link" "api" {
  name               = "${local.name_prefix}-vpclink"
  security_group_ids = [aws_security_group.vpclink.id]
  subnet_ids         = local.vpc_link_subnet_ids
}

# La integración dice A DÓNDE se envía la petición: el servicio de Cloud Map.
# `HTTP_PROXY` reenvía método, ruta, cabeceras y cuerpo tal cual a Spring Boot.
resource "aws_apigatewayv2_integration" "api" {
  api_id             = aws_apigatewayv2_api.api.id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.api.id
  integration_uri    = aws_service_discovery_service.api.arn

  # Las integraciones por VPC Link solo aceptan el formato 1.0.
  payload_format_version = "1.0"
}

# La ruta dice QUÉ peticiones entran. `{proxy+}` captura cualquier ruta, así que
# los controladores de Spring (`/api/v1/...`) siguen decidiendo sus propias rutas.
resource "aws_apigatewayv2_route" "api" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.api.id}"
}

resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/apigw/${local.name_prefix}-api"
  retention_in_days = var.log_retention_days
}

# `$default` es el stage sin prefijo en la URL; `auto_deploy` publica cada cambio
# de rutas e integraciones sin un paso de despliegue manual.
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "$default"
  auto_deploy = true

  # Límite de peticiones por segundo para toda la API (protege el backend de picos
  # o abuso). Pasado el límite, API Gateway responde 429 sin molestar a la tarea.
  default_route_settings {
    throttling_rate_limit  = var.api_throttle_rate_limit
    throttling_burst_limit = var.api_throttle_burst_limit
  }

  # Registro de accesos: una línea JSON por petición. No incluye cuerpos ni cabeceras,
  # así que no guarda tokens ni datos personales.
  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway.arn
    format = jsonencode({
      requestId        = "$context.requestId"
      ip               = "$context.identity.sourceIp"
      requestTime      = "$context.requestTime"
      httpMethod       = "$context.httpMethod"
      path             = "$context.path"
      status           = "$context.status"
      responseLength   = "$context.responseLength"
      integrationError = "$context.integrationErrorMessage"
    })
  }
}
