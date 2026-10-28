# ---------------------------------------------------------------------------
# Lección 5 (2/4): Cloud Map, el "directorio" donde las tareas se registran solas.
#
# ECS registra la IP de cada tarea aquí; en la lección 6, API Gateway (vía VPC
# Link) consulta este servicio para saber a dónde enviar cada petición. Así no
# hace falta un balanceador (ALB), que cobra una cuota fija por hora.
# ---------------------------------------------------------------------------

# Namespace DNS privado: solo se resuelve DENTRO de la VPC. Se llama distinto al
# `aglacita.local` del QA manual porque dos zonas con el mismo nombre en la misma
# VPC (compartida) chocarían.
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "${var.project}.local"
  description = "Descubrimiento de servicios de ${local.name_prefix}"
  vpc         = data.aws_vpc.default.id
}

resource "aws_service_discovery_service" "api" {
  name = "api"

  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.main.id
    routing_policy = "MULTIVALUE"

    # SRV guarda IP *y puerto* (8082); un registro A solo guardaría la IP.
    # TTL corto: si una tarea se reemplaza, el cambio se ve en segundos.
    dns_records {
      type = "SRV"
      ttl  = 15
    }
  }

  # ECS informa a Cloud Map si la tarea está sana; Cloud Map no hace sondeos propios.
  # El bloque vacío `{}` NO se envía a AWS: el servicio nacía sin health check y cada
  # `plan` proponía reemplazarlo. Con `failure_threshold` (obsoleto: AWS siempre usa 1)
  # el bloque sí viaja y el state coincide con la realidad.
  health_check_custom_config {
    failure_threshold = 1
  }
}
