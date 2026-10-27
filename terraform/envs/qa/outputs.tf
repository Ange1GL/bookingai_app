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

output "ecr_repository_url" {
  description = "Repositorio ECR donde subes la imagen de la API."
  value       = aws_ecr_repository.api.repository_url
}

output "ecs_cluster_name" {
  description = "Clúster ECS de la API."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "Servicio ECS de la API."
  value       = aws_ecs_service.api.name
}

output "api_log_group" {
  description = "Grupo de CloudWatch con los logs de la API."
  value       = aws_cloudwatch_log_group.api.name
}

output "cloud_map_service_arn" {
  description = "Servicio de Cloud Map donde se registra la API (lo consumirá API Gateway)."
  value       = aws_service_discovery_service.api.arn
}

output "front_url" {
  description = "URL pública del front."
  value       = "https://${aws_cloudfront_distribution.front.domain_name}"
}
