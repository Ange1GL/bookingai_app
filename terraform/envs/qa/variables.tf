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

variable "front_force_destroy" {
  description = "Permite que `terraform destroy` borre el bucket aunque tenga archivos. Solo para QA."
  type        = bool
  default     = false
}
