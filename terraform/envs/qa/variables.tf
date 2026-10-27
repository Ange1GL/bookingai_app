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
  description = "Unidades de CPU de la tarea Fargate (1024 = 1 vCPU)."
  type        = number
  default     = 1024

  validation {
    condition     = contains([256, 512, 1024, 2048, 4096], var.api_cpu)
    error_message = "api_cpu: valores válidos 256, 512, 1024, 2048 o 4096."
  }
}

variable "api_memory" {
  description = "Memoria de la tarea Fargate en MiB. Debe ser una combinación válida con api_cpu."
  type        = number
  default     = 2048
}

variable "api_container_healthcheck" {
  description = "Añade un health check de contenedor (curl a /actuator/health). Requiere `curl` en la imagen."
  type        = bool
  default     = true
}

variable "api_start_period_seconds" {
  description = "Segundos de gracia tras arrancar el contenedor durante los cuales los fallos del health check no cuentan. Spring Boot tarda en iniciar."
  type        = number
  default     = 90

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
