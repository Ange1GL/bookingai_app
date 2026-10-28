# ---------------------------------------------------------------------------
# Lección 5 (3/4 y 4/4): logs, IAM, clúster, definición de tarea y servicio ECS.
#
#   ECR (imagen) ─┐
#   SSM (config) ─┼─► tarea Fargate "api" ──► CloudWatch Logs
#   Cloud Map  ◄──┘        (api-sg, IP pública)
# ---------------------------------------------------------------------------

data "aws_region" "current" {}

locals {
  api_container_name = "api"
  api_log_group      = "/ecs/${local.name_prefix}-api"

  # Todos los parámetros de la lección 4 (secretos + configuración), por nombre.
  # `merge` une dos mapas; las claves son los nombres de variable de Spring Boot.
  api_parameters = merge(aws_ssm_parameter.secret, aws_ssm_parameter.plain)

  # Formato que pide ECS: cada variable de entorno se LEE de SSM al arrancar la
  # tarea. El valor nunca aparece en la definición de tarea ni en el state.
  api_secrets = [
    for name, parameter in local.api_parameters : {
      name      = name
      valueFrom = parameter.arn
    }
  ]

  api_image = "${aws_ecr_repository.api.repository_url}:${var.api_image_tag}"

  api_container = merge(
    {
      name      = local.api_container_name
      image     = local.api_image
      essential = true # si este contenedor cae, cae la tarea y ECS la reemplaza

      portMappings = [{
        containerPort = var.api_port
        protocol      = "tcp"
      }]

      secrets = local.api_secrets

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.api.name
          "awslogs-region"        = data.aws_region.current.region
          "awslogs-stream-prefix" = "api"
        }
      }
    },
    # `merge` con un mapa vacío = "no pongas esta clave". Es la forma de tener un
    # bloque opcional dentro de un JSON que se construye con `jsonencode`.
    var.api_container_healthcheck ? {
      healthCheck = {
        # /actuator/health es el único endpoint que expone Spring (ver application.yaml).
        # Requiere `curl` en la imagen; si no lo tiene, ponlo en false.
        command     = ["CMD-SHELL", "curl -fs http://localhost:${var.api_port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = var.api_start_period_seconds
      }
    } : {}
  )
}

# --- Logs --------------------------------------------------------------------
# El grupo se crea AQUÍ (y no lo deja crear ECS) para fijar la retención: sin
# ella los logs se guardan para siempre y se cobran para siempre.
resource "aws_cloudwatch_log_group" "api" {
  name              = local.api_log_group
  retention_in_days = var.log_retention_days
}

# --- IAM ---------------------------------------------------------------------
# Hay DOS roles distintos en ECS y se confunden a menudo:
#   - rol de EJECUCIÓN: lo usa ECS (el agente) para preparar la tarea: bajar la
#     imagen, escribir logs, leer los parámetros de SSM.
#   - rol de TAREA: lo usa TU código para llamar a AWS. La API no llama a AWS
#     (usa DeepSeek/OpenAI/WhatsApp por Internet), así que NO se crea ninguno.

# Política de confianza: quién puede "asumir" el rol. Aquí, el servicio ECS Tasks.
data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "api_execution" {
  name               = "${local.name_prefix}-api-execution"
  description        = "Rol de ejecucion de ECS para la API (imagen, logs, SSM)"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

# Política administrada por AWS: permisos para bajar la imagen de ECR y escribir
# en CloudWatch Logs. Es la que usa `ecsTaskExecutionRole` en el QA manual.
resource "aws_iam_role_policy_attachment" "api_execution_managed" {
  role       = aws_iam_role.api_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Lectura de SSM acotada a los parámetros exactos de la API (no a todo SSM). Con
# la clave gestionada `aws/ssm` no se necesita kms:Decrypt; con una clave propia sí.
data "aws_iam_policy_document" "api_execution_ssm" {
  statement {
    sid       = "ReadApiParameters"
    actions   = ["ssm:GetParameters"]
    resources = [for parameter in local.api_parameters : parameter.arn]
  }
}

resource "aws_iam_role_policy" "api_execution_ssm" {
  name   = "read-api-parameters"
  role   = aws_iam_role.api_execution.id
  policy = data.aws_iam_policy_document.api_execution_ssm.json
}

# --- Clúster -----------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  # Container Insights cobra métricas personalizadas; en QA no compensa.
  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

# --- Definición de tarea -----------------------------------------------------
# Es la "receta" (imagen, CPU, memoria, variables). Cada cambio crea una REVISIÓN
# nueva; el servicio apunta a una revisión concreta.
resource "aws_ecs_task_definition" "api" {
  family                   = "${local.name_prefix}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc" # la tarea tiene su propia interfaz de red y IP
  cpu                      = var.api_cpu
  memory                   = var.api_memory
  execution_role_arn       = aws_iam_role.api_execution.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64" # debe coincidir con la arquitectura con que construiste la imagen
  }

  container_definitions = jsonencode([local.api_container])
}

# --- Servicio ----------------------------------------------------------------
# Mantiene `desired_count` tareas vivas y reemplaza las que fallan.
resource "aws_ecs_service" "api" {
  name            = "${local.name_prefix}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  launch_type     = "FARGATE"

  # 0 hasta que subas la imagen y cargues los secretos reales; si no, ECS intenta
  # arrancar una tarea imposible y reintenta sin parar. Ver docs/05-ecs-fargate.md.
  desired_count = var.api_desired_count

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.api.id]
    assign_public_ip = true # sin NAT, la IP pública es lo que le permite llegar a ECR, SSM y a Internet
  }

  # ECS registra/desregistra la tarea en Cloud Map solo. `container_name` y
  # `container_port` son obligatorios con registros SRV: dicen qué puerto anunciar.
  service_registries {
    registry_arn   = aws_service_discovery_service.api.arn
    container_name = local.api_container_name
    container_port = var.api_port
  }

  # Si un despliegue nuevo no logra arrancar, ECS lo da por fallido y vuelve a la
  # revisión anterior, en vez de reintentar indefinidamente.
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # La tarea necesita los permisos del rol AL ARRANCAR, pero ninguna referencia lo
  # expresa (el servicio solo apunta al rol vía la definición de tarea, no a sus
  # políticas). Sin esto, Terraform podría lanzar la tarea antes de que el rol
  # pueda leer SSM. `depends_on` declara esa dependencia a mano.
  depends_on = [
    aws_iam_role_policy_attachment.api_execution_managed,
    aws_iam_role_policy.api_execution_ssm,
  ]

  # El CI (GitHub Actions) registra revisiones nuevas de la tarea y actualiza el servicio.
  # Sin esto, el siguiente `terraform apply` lo devolvería a la revisión que Terraform conoce.
  # Consecuencia: un cambio en la definición de tarea (imagen, CPU, secretos...) ya NO llega al
  # servicio por sí solo con `apply`: Terraform registra la revisión, pero el despliegue lo hace el CI
  # (o `aws ecs update-service`). Mantén .aws/task-definition-backend.json sincronizado con este archivo.
  lifecycle {
    ignore_changes = [task_definition]
  }
}
