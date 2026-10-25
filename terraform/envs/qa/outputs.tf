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
  description = "ARN del bucket (lo usará la política de CloudFront en la lección 2)."
  value       = aws_s3_bucket.front.arn
}
