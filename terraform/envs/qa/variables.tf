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
