# ---------------------------------------------------------------------------
# Lección 4: parámetros en SSM Parameter Store (configuración y secretos de la API)
#
# La tarea de ECS los recibirá como variables de entorno (lección 5). El nombre
# de cada parámetro es el de la variable que lee Spring Boot en application.yaml.
# ---------------------------------------------------------------------------

locals {
  # Todo bajo una misma ruta: el rol de ECS podrá leer solo `/aglacita-tf/qa/*`.
  ssm_path = "/${var.project}/${var.environment}"

  # Variables SIN valor por defecto en application.yaml: si falta una, la app no
  # arranca. Los valores reales NUNCA pasan por Terraform: los carga el script
  # scripts/load-ssm-from-env.mjs (ver docs/04-secretos-ssm.md).
  secret_parameters = toset([
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD",
    "JWT_SECRET",
    "DEEPSEEK_API_KEY",
    "API_OPENAI_KEY",
    "WHATSAPP_VERIFY_TOKEN",
    "WHATSAPP_APP_SECRET",
  ])

  # Opcionales (valor vacío por defecto en application.yaml): sin ellas la app arranca, pero las
  # notificaciones push quedan desactivadas. Terraform no conoce sus valores, así que van como
  # SecureString y las carga el mismo script que los secretos.
  optional_secret_parameters = toset([
    "VAPID_PUBLIC_KEY",
    "VAPID_PRIVATE_KEY",
    "VAPID_SUBJECT",
    "SYSTEM_USER_EMPLOYEE_META",
  ])

  # No son secretos y Terraform ya conoce su valor, así que los gestiona completos.
  # HOST_CORS sale del dominio de CloudFront (lección 2): sin copiar nada a mano.
  plain_parameters = {
    SPRING_PROFILES_ACTIVE = var.environment
    HOST_CORS              = "https://${aws_cloudfront_distribution.front.domain_name}"
  }
}

# `for_each` sobre un conjunto: un recurso por cada nombre, con la clave en el state
# (aws_ssm_parameter.secret["JWT_SECRET"]).
resource "aws_ssm_parameter" "secret" {
  for_each = setunion(local.secret_parameters, local.optional_secret_parameters)

  name        = "${local.ssm_path}/${each.key}"
  description = "Secreto ${each.key} de la API. Valor cargado fuera de Terraform."
  type        = "SecureString" # cifrado con la clave gratuita aws/ssm

  # `value_wo` es de SOLO ESCRITURA: se envía a AWS pero Terraform jamás lo guarda
  # en el state ni en el plan. Con `value` el secreto real quedaría en texto plano
  # en terraform.tfstate. Aquí solo va un relleno para que el parámetro exista.
  value_wo         = local.ssm_placeholder
  value_wo_version = 1 # súbelo solo si quieres que Terraform reenvíe el relleno
}

resource "aws_ssm_parameter" "plain" {
  for_each = local.plain_parameters

  name        = "${local.ssm_path}/${each.key}"
  description = "Configuración ${each.key} de la API."
  type        = "String"
  value       = each.value
}
