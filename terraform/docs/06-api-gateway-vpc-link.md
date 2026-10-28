# Lección 6: API Gateway + VPC Link

Hasta ahora la API solo es alcanzable desde la VPC. Aquí construyes el camino público: el navegador llama a
`https://<cloudfront>/api/v1/...`, CloudFront reenvía a API Gateway, y este llega a la tarea por un túnel
privado. El código está en [`api_gateway.tf`](../envs/qa/api_gateway.tf) y
[`cloudfront.tf`](../envs/qa/cloudfront.tf).

```
navegador ──HTTPS──► CloudFront ──┬─ /*      ──► S3 privado (front)         [lección 2]
                                  └─ /api/*  ──► API Gateway (HTTP API)
                                                    │ VPC Link (vpclink-sg)
                                                    ▼
                                      Cloud Map api.aglacita-tf.local        [lección 5]
                                                    ▼
                                      tarea Fargate :8082 (api-sg)           [lección 5]
```

## Antes de empezar: permisos de IAM

Esta lección usa servicios nuevos (API Gateway, CloudFront Functions, entrega de logs). Crea la política
`terraform-aglacita-tf-apigw` en **IAM → Políticas → Crear política** y adjúntala a `angel`:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "ApiGateway",
			"Effect": "Allow",
			"Action": ["apigateway:GET", "apigateway:POST", "apigateway:PUT", "apigateway:PATCH", "apigateway:DELETE", "apigateway:TagResource", "apigateway:UntagResource"],
			"Resource": [
				"arn:aws:apigateway:*::/apis",
				"arn:aws:apigateway:*::/apis/*",
				"arn:aws:apigateway:*::/vpclinks",
				"arn:aws:apigateway:*::/vpclinks/*",
				"arn:aws:apigateway:*::/tags/*"
			]
		},
		{
			"Sid": "VpcLinkNetwork",
			"Effect": "Allow",
			"Action": [
				"ec2:DescribeSubnets",
				"ec2:DescribeSecurityGroups",
				"ec2:DescribeVpcs",
				"ec2:CreateNetworkInterface",
				"ec2:CreateNetworkInterfacePermission",
				"ec2:DeleteNetworkInterface",
				"ec2:DescribeNetworkInterfaces",
				"ec2:ModifyNetworkInterfaceAttribute",
				"ec2:CreateTags"
			],
			"Resource": "*"
		},
		{
			"Sid": "ApiGatewayServiceLinkedRole",
			"Effect": "Allow",
			"Action": "iam:CreateServiceLinkedRole",
			"Resource": "arn:aws:iam::*:role/aws-service-role/ops.apigateway.amazonaws.com/*",
			"Condition": { "StringLike": { "iam:AWSServiceName": "ops.apigateway.amazonaws.com" } }
		},
		{
			"Sid": "ApiGatewayLogGroup",
			"Effect": "Allow",
			"Action": [
				"logs:CreateLogGroup",
				"logs:DeleteLogGroup",
				"logs:PutRetentionPolicy",
				"logs:TagResource",
				"logs:UntagResource",
				"logs:ListTagsForResource",
				"logs:TagLogGroup",
				"logs:UntagLogGroup",
				"logs:ListTagsLogGroup"
			],
			"Resource": "arn:aws:logs:*:*:log-group:/apigw/aglacita-tf-*"
		},
		{
			"Sid": "ApiGatewayLogDelivery",
			"Effect": "Allow",
			"Action": [
				"logs:CreateLogDelivery",
				"logs:GetLogDelivery",
				"logs:UpdateLogDelivery",
				"logs:DeleteLogDelivery",
				"logs:ListLogDeliveries",
				"logs:PutResourcePolicy",
				"logs:DescribeResourcePolicies"
			],
			"Resource": "*"
		},
		{
			"Sid": "CloudFrontFunctionsAndPolicies",
			"Effect": "Allow",
			"Action": [
				"cloudfront:CreateFunction",
				"cloudfront:DescribeFunction",
				"cloudfront:GetFunction",
				"cloudfront:PublishFunction",
				"cloudfront:UpdateFunction",
				"cloudfront:DeleteFunction",
				"cloudfront:ListFunctions",
				"cloudfront:ListOriginRequestPolicies",
				"cloudfront:GetOriginRequestPolicy"
			],
			"Resource": "*"
		}
	]
}
```

Esta lista se armó a partir de la documentación y no se ha probado contra AWS. Si `terraform apply` falla con
`AccessDenied`, el mensaje nombra la acción que falta: añádela a la política y repite. Es el flujo normal al
trabajar con mínimo privilegio.

## Conceptos

- **HTTP API vs REST API:** la HTTP API es más simple y barata (se cobra por petición); la REST API tiene
  más funciones (WAF, claves de uso). Para QA basta la HTTP API.
- **VPC Link:** API Gateway vive fuera de tu VPC; el VPC Link crea interfaces de red en tus subredes,
  protegidas por `vpclink-sg` (lección 3), para llegar a la tarea privada.
- **Integración, ruta y stage:** la integración dice *a dónde* enviar, la ruta *qué peticiones* entran y el
  stage *la versión publicada*. Con `$default` y `auto_deploy` no hay paso de despliegue manual.
- **Integración con Cloud Map:** API Gateway pregunta a Cloud Map por la IP y el puerto vivos (registro SRV).
  Por eso no hace falta un ALB con cuota fija por hora.
- **`payload_format_version = "1.0"`:** las integraciones por VPC Link solo aceptan este formato.
- **Throttling:** `default_route_settings` limita las peticiones por segundo de toda la API; el exceso recibe
  429 sin llegar a la tarea. Se ajusta con `api_throttle_rate_limit` y `api_throttle_burst_limit`.
- **`replace` sobre un atributo:** la URL de la API trae `https://`, pero CloudFront pide solo el dominio:
  `replace(aws_apigatewayv2_api.api.api_endpoint, "https://", "")`.

## Cambios en CloudFront

- **Segundo origen** `api-gateway` (solo HTTPS) y comportamiento `ordered_cache_behavior` para `/api/*`: todos
  los métodos, **sin caché** (`Managed-CachingDisabled`) y reenviando cabeceras, cookies y query strings
  salvo el `Host` (`Managed-AllViewerExceptHostHeader`; API Gateway rechaza un `Host` ajeno).
- **`custom_error_response` sustituido por una CloudFront Function.** Ese bloque se aplica a *toda* la
  distribución: con la API detrás, un 403 o 404 de Spring habría llegado al navegador como `index.html` con
  código 200. La función reescribe solo las rutas sin extensión (`/dashboard`) a `/index.html` en la
  petición al front.

## Arreglo en Cloud Map

El bloque vacío `health_check_custom_config {}` de la lección 5 no se enviaba a AWS: el servicio existía sin
health check y cada `plan` proponía reemplazarlo. Ahora lleva `failure_threshold = 1` (obsoleto, pero AWS
siempre usa 1). **El primer `apply` reemplazará el servicio de Cloud Map** y, como el servicio de ECS lo
referencia, también el de ECS. Con `desired_count = 0` no hay tareas que perder.

## Zonas de disponibilidad no soportadas por el VPC Link

API Gateway no ofrece VPC Links en todas las zonas. En esta cuenta `CreateVpcLink` falló con *"Subnet ... is in
Availability Zone 'use1-az3' where service is not available"* (`use1-az3` es `us-east-1e`). Como la VPC default
tiene una subred por zona, `api_gateway.tf` lee cada subred con `data "aws_subnet"` + `for_each` y descarta las
zonas de `vpc_link_unsupported_az_ids` (por defecto `["use1-az3"]`). Se usa el **id** de zona y no el nombre,
porque `us-east-1e` puede corresponder a otra zona física en otra cuenta.

## Seguridad: la URL directa de API Gateway sigue siendo pública

Cualquiera que conozca la URL `execute-api` puede llamarla sin pasar por CloudFront. Para QA se acepta: los
endpoints exigen la autenticación de la aplicación. Una mejora posterior es un header secreto que añade
CloudFront y valida el backend (un filtro en Spring, con el secreto en SSM).

## Comandos

```bash
cd terraform/envs/qa
terraform fmt
terraform validate
terraform plan
terraform apply
```

Con la tarea viva (`api_desired_count = 1`):

```bash
curl https://<front_url>/api/v1/<endpoint público>
```

## Qué observar

1. Consola → **API Gateway → API**: `aglacita-tf-qa-api`. En **Enlaces de VPC**: `aglacita-tf-qa-vpclink`
   (junto al `aglacita-qa-vpclink` del QA manual).
2. **Integraciones** de la API: tipo VPC Link con destino el servicio `api` de Cloud Map.
3. **CloudWatch → Grupos de registros → `/apigw/aglacita-tf-qa-api`**: una línea JSON por petición.
4. **Cloud Map → `aglacita-tf.local` → `api`**: debe haber una instancia viva; si no, la API responde 503.
5. CloudFront tarda unos minutos en propagar los nuevos comportamientos.
6. Recarga `/dashboard` en el front: debe cargar la SPA. Una ruta inexistente bajo `/api/` debe devolver el
   404 de Spring, no `index.html`.

## Costo

API Gateway HTTP cobra por petición y CloudFront Functions tiene una cuota gratuita amplia; el costo extra en
QA es bajo, pero **no se midió** para esta lección.

## Limpiar

`terraform destroy` borra todo, incluido el VPC Link.

## Ejercicios

1. Baja `api_throttle_rate_limit` a `1` y lanza 10 peticiones seguidas con `curl`. ¿Qué código ves? ¿Y qué
   aparece en el log de acceso?
2. Pon `api_throttle_burst_limit` menor que el rate limit. ¿Qué detecta el `plan` y en qué momento?
3. ¿Por qué la función de CloudFront usa `indexOf('.')`? ¿Qué pasaría con `/assets/logo.svg` sin esa condición?
4. Investiga cómo validar un header secreto de CloudFront en un filtro de Spring (`OncePerRequestFilter`).

Siguiente: Lección 7, módulos y remote state.
