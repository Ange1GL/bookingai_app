# ---------------------------------------------------------------------------
# Lección 3: security groups con mínimo privilegio.
#
# Cadena de confianza:  API Gateway ─► VPC Link (vpclink-sg) ─► tarea (api-sg)
# Un security group es un firewall STATEFUL: si permites la entrada, la respuesta
# vuelve sola; solo hay que pensar en quién inicia la conexión.
# ---------------------------------------------------------------------------

# Grupo vacío: las reglas van aparte (recursos `aws_vpc_security_group_*_rule`).
# Declararlas dentro del grupo (bloques `ingress`/`egress`) es el estilo antiguo:
# Terraform pasa a "dueño absoluto" de las reglas y se pelea con las reglas
# sueltas. Una regla = un recurso es más claro y permite referenciar grupos entre sí.
resource "aws_security_group" "vpclink" {
  name        = "${local.name_prefix}-vpclink-sg"
  description = "Interfaces del VPC Link de API Gateway"
  vpc_id      = data.aws_vpc.default.id

  tags = {
    Name = "${local.name_prefix}-vpclink-sg"
  }
}

resource "aws_security_group" "api" {
  name        = "${local.name_prefix}-api-sg"
  description = "Tarea Fargate de la API Spring Boot"
  vpc_id      = data.aws_vpc.default.id

  tags = {
    Name = "${local.name_prefix}-api-sg"
  }
}

# --- vpclink-sg -------------------------------------------------------------
# Sin reglas de entrada: nadie inicia conexiones hacia el VPC Link. Sin ninguna
# regla, un security group deniega todo. Solo permitimos SALIR hacia la API, y
# únicamente al puerto de la app. (El QA manual abría la salida a 0.0.0.0/0.)
resource "aws_vpc_security_group_egress_rule" "vpclink_to_api" {
  security_group_id = aws_security_group.vpclink.id
  description       = "VPC Link hacia la API"

  # En vez de una IP/CIDR, el destino es OTRO security group: vale para cualquier
  # tarea que lo lleve, aunque su IP cambie en cada despliegue.
  referenced_security_group_id = aws_security_group.api.id
  ip_protocol                  = "tcp"
  from_port                    = var.api_port
  to_port                      = var.api_port
}

# --- api-sg -----------------------------------------------------------------
# Entrada: SOLO el VPC Link, y SOLO el puerto de la app. Se quitan las dos
# reglas "de más" del QA manual (todo el CIDR de la VPC y una IP fija).
resource "aws_vpc_security_group_ingress_rule" "api_from_vpclink" {
  security_group_id = aws_security_group.api.id
  description       = "API desde el VPC Link"

  referenced_security_group_id = aws_security_group.vpclink.id
  ip_protocol                  = "tcp"
  from_port                    = var.api_port
  to_port                      = var.api_port
}

# Acceso directo temporal para pruebas desde tus IPs (lista vacía = ninguno).
# `for_each` crea UNA regla por cada CIDR; la clave es el propio CIDR, así que
# quitar uno de la lista destruye solo esa regla y no toca las demás.
resource "aws_vpc_security_group_ingress_rule" "api_debug" {
  for_each = toset(var.api_debug_cidrs)

  security_group_id = aws_security_group.api.id
  description       = "Acceso directo de depuracion"

  cidr_ipv4   = each.value
  ip_protocol = "tcp"
  from_port   = var.api_port
  to_port     = var.api_port
}

# Salida: la tarea está en subred pública y sin NAT, así que ella misma debe
# alcanzar Internet para descargar la imagen de ECR, leer SSM, llamar a DeepSeek
# y conectarse a la base de datos. `-1` = todos los protocolos y puertos.
# Se podría limitar a 443 + puerto de la BD; queda como ejercicio.
resource "aws_vpc_security_group_egress_rule" "api_all_out" {
  security_group_id = aws_security_group.api.id
  description       = "Salida a Internet (ECR, SSM, APIs externas, BD)"

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"
}

# --- Grupo `default` (OPCIONAL) ---------------------------------------------
# Todas las VPC traen un grupo `default`. En el QA manual permite TCP 80 desde
# todo Internet (la regla más expuesta del documento). Terraform no puede
# "borrar" ese grupo, pero `aws_default_security_group` lo ADOPTA: al no declarar
# reglas, las deja en cero.
#
# Está apagado por defecto porque la VPC default es COMPARTIDA con el QA manual y
# no se verificó qué recursos usan ese grupo. Actívalo solo tras comprobarlo.
resource "aws_default_security_group" "default" {
  count = var.harden_default_sg ? 1 : 0

  vpc_id = data.aws_vpc.default.id
}
