# Los outputs se imprimen al terminar `apply` y otros módulos pueden consumirlos.

output "account_id" {
  description = "Cuenta de AWS en la que Terraform está trabajando."
  value       = data.aws_caller_identity.current.account_id
}

output "front_bucket_name" {
  description = "Bucket S3 privado del front-end."
  value       = aws_s3_bucket.front.id
}

output "front_bucket_arn" {
  description = "ARN del bucket."
  value       = aws_s3_bucket.front.arn
}

output "front_distribution_id" {
  description = "Id de la distribución (sirve para invalidar caché en los despliegues)."
  value       = aws_cloudfront_distribution.front.id
}

output "vpc_id" {
  description = "VPC por defecto en la que se crean los security groups."
  value       = data.aws_vpc.default.id
}

output "subnet_ids" {
  description = "Subredes de la VPC (las usarán ECS y el VPC Link)."
  value       = data.aws_subnets.default.ids
}

output "vpclink_security_group_id" {
  description = "Security group del VPC Link."
  value       = aws_security_group.vpclink.id
}

output "api_security_group_id" {
  description = "Security group de la tarea Fargate."
  value       = aws_security_group.api.id
}

output "ssm_parameter_path" {
  description = "Ruta de los parámetros de la API (el rol de ECS leerá solo esta ruta)."
  value       = local.ssm_path
}

output "front_url" {
  description = "URL pública del front."
  value       = "https://${aws_cloudfront_distribution.front.domain_name}"
}
