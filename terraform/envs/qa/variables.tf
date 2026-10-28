variable "aws_region" {
  description = "Región de AWS donde se crea todo."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Prefijo de nombres. Distinto al del QA manual (aglacita-qa) para no chocar."
  type        = string
  default     = "aglacita-tf"

  validation {
    condition     = can(regex("^[a-z0-9-]{3,30}$", var.project))
    error_message = "project: solo minúsculas, números y guiones (3 a 30 caracteres); S3 no admite otros."
  }
}

variable "environment" {
  description = "Nombre del entorno."
  type        = string
  default     = "qa"
}

variable "api_port" {
  description = "Puerto TCP en el que escucha el contenedor de la API (EXPOSE del Dockerfile del backend)."
  type        = number
  default     = 8082

  validation {
    condition     = var.api_port >= 1024 && var.api_port <= 65535
    error_message = "api_port: debe estar entre 1024 y 65535."
  }
}

variable "api_debug_cidrs" {
  description = "CIDRs con acceso directo al puerto de la API, solo para depurar. Vacío = ninguno."
  type        = list(string)
  default     = []

  validation {
    condition     = alltrue([for cidr in var.api_debug_cidrs : can(cidrnetmask(cidr)) && cidr != "0.0.0.0/0"])
    error_message = "api_debug_cidrs: cada elemento debe ser un CIDR IPv4 válido (p. ej. 203.0.113.7/32) y nunca 0.0.0.0/0."
  }
}

variable "harden_default_sg" {
  description = "Vacía las reglas del security group `default` de la VPC (compartido con el QA manual). Verifica antes qué lo usa."
  type        = bool
  default     = false
}

variable "front_force_destroy" {
  description = "Permite que `terraform destroy` borre el bucket aunque tenga archivos. Solo para QA."
  type        = bool
  default     = false
}

variable "ecr_force_delete" {
  description = "Permite que `terraform destroy` borre el repositorio ECR aunque tenga imágenes. Solo para QA."
  type        = bool
  default     = false
}

variable "ecr_keep_images" {
  description = "Cuántas imágenes conserva el repositorio ECR; las más antiguas caducan."
  type        = number
  default     = 10

  validation {
    condition     = var.ecr_keep_images >= 1
    error_message = "ecr_keep_images: debe ser al menos 1."
  }
}

variable "api_image_tag" {
  description = "Etiqueta de la imagen de la API en ECR (p. ej. el hash corto del commit). El repositorio es inmutable: cada despliegue usa una etiqueta nueva."
  type        = string
  default     = "initial"
}

variable "api_desired_count" {
  description = "Tareas de la API. Déjalo en 0 hasta subir la imagen a ECR y cargar los secretos reales."
  type        = number
  default     = 0

  validation {
    condition     = var.api_desired_count >= 0 && var.api_desired_count <= 2
    error_message = "api_desired_count: entre 0 y 2 (es un entorno QA)."
  }
}

variable "api_cpu" {
  description = "Unidades de CPU de la tarea Fargate (1024 = 1 vCPU). 256 es el mínimo de Fargate."
  type        = number
  default     = 256

  validation {
    condition     = contains([256, 512, 1024, 2048, 4096], var.api_cpu)
    error_message = "api_cpu: valores válidos 256, 512, 1024, 2048 o 4096."
  }
}

variable "api_memory" {
  description = "Memoria de la tarea Fargate en MiB. 512 es el mínimo de Fargate; debe ser una combinación válida con api_cpu."
  type        = number
  default     = 512

  # Combinaciones que admite Fargate (Linux) para cada CPU.
  validation {
    condition = contains(lookup({
      256  = [512, 1024, 2048]
      512  = [1024, 2048, 3072, 4096]
      1024 = [2048, 3072, 4096, 5120, 6144, 7168, 8192]
      2048 = [for gb in range(4, 17) : gb * 1024]
      4096 = [for gb in range(8, 31) : gb * 1024]
    }, var.api_cpu, []), var.api_memory)
    error_message = "api_memory: combinación no válida con api_cpu en Fargate (p. ej. 256 de CPU admite 512, 1024 o 2048 MiB)."
  }
}

variable "api_container_healthcheck" {
  description = "Añade un health check de contenedor (curl a /actuator/health). Requiere `curl` en la imagen."
  type        = bool
  default     = true
}

variable "api_start_period_seconds" {
  description = "Segundos de gracia tras arrancar el contenedor durante los cuales los fallos del health check no cuentan. Spring Boot tarda en iniciar."
  type        = number
  default     = 240 # 0,25 vCPU arranca Spring Boot despacio

  validation {
    condition     = var.api_start_period_seconds >= 0 && var.api_start_period_seconds <= 300
    error_message = "api_start_period_seconds: entre 0 y 300 (límite de ECS)."
  }
}

variable "log_retention_days" {
  description = "Días que CloudWatch conserva los logs de la API."
  type        = number
  default     = 14

  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365], var.log_retention_days)
    error_message = "log_retention_days: CloudWatch solo admite 1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180 o 365."
  }
}

variable "api_throttle_rate_limit" {
  description = "Peticiones por segundo que admite API Gateway en total; por encima responde 429."
  type        = number
  default     = 50

  validation {
    condition     = var.api_throttle_rate_limit >= 1
    error_message = "api_throttle_rate_limit: debe ser al menos 1."
  }
}

variable "api_throttle_burst_limit" {
  description = "Pico de peticiones simultáneas que tolera API Gateway antes de aplicar el límite por segundo."
  type        = number
  default     = 100

  validation {
    condition     = var.api_throttle_burst_limit >= var.api_throttle_rate_limit
    error_message = "api_throttle_burst_limit: no puede ser menor que api_throttle_rate_limit."
  }
}
