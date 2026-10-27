# Lección 5: ECS Fargate + Cloud Map + logs

Aquí pones a correr la API. Es la lección con más piezas: un repositorio de imágenes, un directorio de
servicios, logs, permisos y el servicio que mantiene la tarea viva. El código está en
[`ecr.tf`](../envs/qa/ecr.tf), [`service_discovery.tf`](../envs/qa/service_discovery.tf) y
[`ecs.tf`](../envs/qa/ecs.tf).

```
                    ┌──────────── rol de ejecución (IAM) ─────────────┐
 ECR (imagen) ──────┤                                                 │
 SSM (/aglacita-tf/qa/*) ───────────► tarea Fargate "api" :8082 ──► CloudWatch Logs
                                       api-sg · IP pública (lección 3)
                                              │ se registra sola
                                              ▼
                              Cloud Map  api.aglacita-tf.local  (lo consume API Gateway en la lección 6)
```

## El orden importa: Terraform crea, tú subes la imagen

Terraform crea el repositorio ECR **vacío**. Si el servicio pidiera una tarea sin imagen, ECS fallaría y
reintentaría sin parar. Por eso `api_desired_count` vale `0` por defecto: el servicio y todo lo demás
existen, pero no arranca ninguna tarea. Cuando la imagen y los secretos reales estén listos, lo subes a `1`.

1. `terraform apply` → infraestructura creada, 0 tareas.
2. Construyes y subes la imagen a ECR.
3. Cargas los secretos reales (script de la lección 4).
4. `api_desired_count = 1` y `api_image_tag = "<etiqueta>"` → `terraform apply` → la tarea arranca.

## Conceptos de Terraform

### `jsonencode` para `container_definitions`

```hcl
container_definitions = jsonencode([local.api_container])
```

ECS espera un texto JSON. Escribirlo a mano obliga a escapar comillas y no valida nada; con `jsonencode`
armas un objeto HCL (con variables y referencias) y Terraform lo convierte. Ya lo usaste en la política de
CloudFront (lección 2).

### Bloques opcionales con `merge`

```hcl
var.api_container_healthcheck ? { healthCheck = { ... } } : {}
```

Dentro de un JSON no existe `count` ni `dynamic`. Mezclar con un mapa vacío es la forma de "incluir esta
clave solo si...".

### `for` para transformar mapas en listas

```hcl
api_secrets = [for name, parameter in local.api_parameters : { name = name, valueFrom = parameter.arn }]
```

Convierte los 10 parámetros de la lección 4 en la lista `[{name, valueFrom}]` que pide ECS. Añadir un
parámetro nuevo a `secret_parameters` lo inyecta en la tarea sin tocar este archivo. `merge` une los dos
mapas (`secret` y `plain`); ambos tienen el nombre de variable como clave.

### `depends_on` cuando la dependencia no se ve

```hcl
depends_on = [
  aws_iam_role_policy_attachment.api_execution_managed,
  aws_iam_role_policy.api_execution_ssm,
]
```

Terraform deduce el orden de las **referencias**. El servicio llega al rol por la definición de tarea, pero
nunca nombra las políticas adjuntas al rol. Sin `depends_on`, la tarea podría lanzarse antes de que el rol
tenga permiso para leer SSM y fallaría en el primer arranque. En la lección 1 viste el mismo caso con el
ciclo de vida del bucket.

### `data "aws_iam_policy_document"`

Genera el JSON de una política desde bloques HCL (`statement`, `actions`, `resources`). Termina en
`.json`, igual de válido que un `jsonencode`, pero con comprobación de nombres de atributo.

## Qué se crea

| Recurso | Para qué |
|---|---|
| `aws_ecr_repository` + `aws_ecr_lifecycle_policy` | Repositorio de imágenes, inmutable, con escaneo, que conserva las últimas 10. |
| `aws_cloudwatch_log_group` | Logs de la API con retención de 14 días. |
| `aws_iam_role` + attachment + política en línea | Rol de ejecución: baja la imagen, escribe logs, lee los 10 parámetros. |
| `aws_service_discovery_private_dns_namespace` + `_service` | `aglacita-tf.local` y su servicio `api` (registros SRV, TTL 15). |
| `aws_ecs_cluster` | Contenedor lógico, sin Container Insights (tiene costo). |
| `aws_ecs_task_definition` | Receta: 1 vCPU, 2 GB, imagen, secretos, logs, health check. |
| `aws_ecs_service` | Mantiene `desired_count` tareas; circuit breaker; se registra en Cloud Map. |

## Decisiones de seguridad y por qué

- **Tags inmutables:** `abc1234` siempre es la misma imagen. Con `latest` mutable, un `apply` podría
  desplegar código distinto sin que el plan lo muestre. El precio: cada despliegue necesita una etiqueta
  nueva (el hash del commit sirve).
- **Rol de ejecución vs. rol de tarea:** el de ejecución lo usa ECS para *preparar* la tarea; el de tarea lo
  usa *tu código* para llamar a AWS. La API solo llama a servicios externos por Internet, así que no hay rol
  de tarea: si alguien ejecuta código dentro del contenedor, no obtiene credenciales de AWS.
- **SSM acotado a 10 ARN exactos**, no a `/aglacita-tf/qa/*` ni a todo SSM. Con la clave gestionada
  `aws/ssm` no hace falta `kms:Decrypt`; si algún día usas una clave propia, hay que añadirlo.
- **Retención de logs:** sin ella CloudWatch guarda para siempre y cobra para siempre.
- **Circuit breaker:** un despliegue que no arranca vuelve a la revisión anterior en vez de reintentar sin
  fin.
- **Sin período de gracia del servicio:** `health_check_grace_period_seconds` solo existe para servicios con
  balanceador, y aquí no hay (Cloud Map no lo admite). Su equivalente es `startPeriod` del health check del
  **contenedor** (`api_start_period_seconds`, 90 s): durante ese tiempo Spring Boot puede seguir arrancando
  sin que ECS lo mate.
- **IP pública en la tarea:** sin NAT (decisión de la lección 3) es lo que le permite llegar a ECR, SSM y a
  la base de datos. El `api-sg` sigue cerrado: solo el VPC Link entra por el 8082.

### Health check de contenedor

Usa `curl -fs http://localhost:8082/actuator/health`. Requiere `curl` en la imagen. Compruébalo antes:

```bash
docker run --rm --entrypoint curl <imagen> --version
```

Si no existe, pon `api_container_healthcheck = false` (o instálalo en el `Dockerfile`). Un health check
que siempre falla mata la tarea en bucle.

## Permisos de IAM necesarios

Crea la política desde **IAM → Políticas → Crear política** con el nombre `terraform-aglacita-tf-ecs` y
adjúntala a `angel`:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "Ecr",
			"Effect": "Allow",
			"Action": "ecr:*",
			"Resource": "arn:aws:ecr:*:*:repository/aglacita-tf-*"
		},
		{
			"Sid": "EcrLogin",
			"Effect": "Allow",
			"Action": "ecr:GetAuthorizationToken",
			"Resource": "*"
		},
		{
			"Sid": "Logs",
			"Effect": "Allow",
			"Action": [
				"logs:CreateLogGroup",
				"logs:DeleteLogGroup",
				"logs:PutRetentionPolicy",
				"logs:TagResource",
				"logs:UntagResource",
				"logs:ListTagsForResource"
			],
			"Resource": "arn:aws:logs:*:*:log-group:/ecs/aglacita-tf-*"
		},
		{
			"Sid": "LogsDescribe",
			"Effect": "Allow",
			"Action": "logs:DescribeLogGroups",
			"Resource": "*"
		},
		{
			"Sid": "IamRolesOfTheProject",
			"Effect": "Allow",
			"Action": [
				"iam:CreateRole",
				"iam:DeleteRole",
				"iam:GetRole",
				"iam:UpdateRole",
				"iam:TagRole",
				"iam:UntagRole",
				"iam:ListRoleTags",
				"iam:UpdateAssumeRolePolicy",
				"iam:AttachRolePolicy",
				"iam:DetachRolePolicy",
				"iam:ListAttachedRolePolicies",
				"iam:PutRolePolicy",
				"iam:GetRolePolicy",
				"iam:DeleteRolePolicy",
				"iam:ListRolePolicies",
				"iam:ListInstanceProfilesForRole",
				"iam:PassRole"
			],
			"Resource": "arn:aws:iam::*:role/aglacita-tf-*"
		},
		{
			"Sid": "Ecs",
			"Effect": "Allow",
			"Action": [
				"ecs:CreateCluster",
				"ecs:DeleteCluster",
				"ecs:UpdateCluster",
				"ecs:UpdateClusterSettings",
				"ecs:CreateService",
				"ecs:UpdateService",
				"ecs:DeleteService",
				"ecs:TagResource",
				"ecs:UntagResource"
			],
			"Resource": [
				"arn:aws:ecs:*:*:cluster/aglacita-tf-*",
				"arn:aws:ecs:*:*:service/aglacita-tf-*/*"
			]
		},
		{
			"Sid": "EcsNoResourceLevel",
			"Effect": "Allow",
			"Action": [
				"ecs:DescribeClusters",
				"ecs:DescribeServices",
				"ecs:DescribeTaskDefinition",
				"ecs:ListTagsForResource",
				"ecs:ListTasks",
				"ecs:DescribeTasks",
				"ecs:RegisterTaskDefinition",
				"ecs:DeregisterTaskDefinition"
			],
			"Resource": "*"
		},
		{
			"Sid": "CloudMapAndItsPrivateZone",
			"Effect": "Allow",
			"Action": [
				"servicediscovery:*",
				"route53:CreateHostedZone",
				"route53:DeleteHostedZone",
				"route53:GetHostedZone",
				"route53:ListHostedZonesByName",
				"route53:ChangeResourceRecordSets",
				"route53:ListResourceRecordSets",
				"route53:GetChange",
				"route53:ChangeTagsForResource",
				"route53:ListTagsForResource",
				"ec2:DescribeVpcs",
				"ec2:DescribeRegions"
			],
			"Resource": "*"
		},
		{
			"Sid": "EcsServiceLinkedRole",
			"Effect": "Allow",
			"Action": "iam:CreateServiceLinkedRole",
			"Resource": "arn:aws:iam::*:role/aws-service-role/ecs.amazonaws.com/*",
			"Condition": { "StringLike": { "iam:AWSServiceName": "ecs.amazonaws.com" } }
		}
	]
}
```

Fíjate en lo que **no** se puede acotar: la API de ECS no admite `Resource` por recurso en las acciones de
definición de tarea ni en las `Describe*`, y Cloud Map crea una zona de Route 53 por dentro, así que esas
dos partes van con `*`. Lo que sí se acota (ECR, logs, roles IAM, clúster, servicio) queda limitado al
prefijo `aglacita-tf-`. `iam:PassRole` limitado a esos roles evita que `angel` entregue a ECS un rol más
poderoso. La acción `ecr:*` sobre el repositorio incluye subir imágenes; si prefieres separarlo, usa solo las
de lectura/escritura que necesites.

## Ejecutar

```bash
cd terraform/envs/qa
terraform plan
```

Esperado: `Plan: 11 to add, 0 to change, 0 to destroy.` Lee el `plan` de `aws_ecs_task_definition`: en
`container_definitions` verás los `valueFrom` (ARN de SSM), nunca un valor de secreto.

```bash
terraform apply     # escribe yes
```

El servicio queda con `desired 0 / running 0`. Es lo esperado.

### Subir la imagen

Desde la carpeta `backend/`, con Docker en marcha:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account_id>.dkr.ecr.us-east-1.amazonaws.com
docker build --platform linux/amd64 -t <ecr_repository_url>:<etiqueta> .
docker push <ecr_repository_url>:<etiqueta>
```

`<ecr_repository_url>` sale de `terraform output ecr_repository_url`, y `<etiqueta>` puede ser
`git rev-parse --short HEAD`. `--platform linux/amd64` importa si construyes en un Mac con chip Apple: la
tarea es `X86_64` y una imagen ARM no arrancaría.

### Arrancar la tarea

Con los secretos ya cargados (lección 4, `--apply`), edita `terraform.tfvars`:

```hcl
api_image_tag     = "abc1234"
api_desired_count = 1
```

```bash
terraform apply
```

Solo cambia la definición de tarea (nueva revisión) y el `desired_count` del servicio.

## Qué observar

1. `terraform state show aws_ecs_task_definition.api`: en `container_definitions` aparece `valueFrom` con el
   ARN, pero ningún secreto.
2. En la consola: ECS → Clústeres → `aglacita-tf-qa-cluster` → Servicios → pestaña **Eventos**. Si la tarea
   no arranca, la causa aparece ahí (`ResourceInitializationError` suele ser ECR o SSM inaccesibles).
3. CloudWatch → Grupos de registros → `/ecs/aglacita-tf-qa-api`: la salida de Spring Boot. Si ves
   `Started ...Application`, funciona.
4. Cloud Map → Namespaces → `aglacita-tf.local` → servicio `api`: con la tarea viva aparece **una instancia**
   con la IP privada y el puerto 8082. Se borra sola cuando la tarea muere.
5. Prueba de humo directa (necesita `api_debug_cidrs = ["<tu-ip>/32"]`): `curl http://<ip pública de la tarea>:8082/actuator/health`
   → `{"status":"UP"}`. Quita el CIDR al terminar.
6. Sube `api_image_tag` a una etiqueta que **no existe** y mira cómo el circuit breaker marca el despliegue
   como fallido y conserva la revisión que funcionaba.

## Costo

Con la tarea apagada (`desired_count = 0`) el costo es casi cero: ECR, logs y Cloud Map cobran por uso
mínimo. Encendida (1 vCPU, 2 GB, IP pública) son unos **~40 USD/mes** (~1,3 USD/día); la palanca principal
es volver a `0` cuando QA no se use. Los costos de ECR y de Cloud Map no se midieron.

## Limpiar

```bash
terraform destroy
```

El `destroy` es seguro porque `ecr_force_delete = true` (en `terraform.tfvars`) permite borrar el repositorio
con imágenes. Si prefieres apagar sin destruir: `api_desired_count = 0` y `terraform apply`.

## Ejercicios

1. Cambia `api_cpu`/`api_memory` a `512`/`1024`. ¿Qué recursos reemplaza el `plan`? (Pista: una revisión nueva
   de la definición de tarea y la actualización del servicio; no se destruye el clúster.)
2. Pon `api_memory = 3000` con `api_cpu = 1024`. Fargate solo admite ciertas combinaciones: ¿en qué momento
   falla, en `plan` o en `apply`? ¿Cómo lo moverías a una `validation`?
3. Fargate Spot es ~70 % más barato. Investiga `aws_ecs_cluster_capacity_providers` y
   `capacity_provider_strategy`, y cambia `launch_type` por una estrategia con `FARGATE_SPOT`. ¿Qué pasa
   con la tarea si AWS recupera la capacidad?
4. Pregunta: ¿por qué `aws_ecs_service.api` necesita `depends_on` y `aws_ecs_task_definition.api` no?

Siguiente: Lección 6, API Gateway + VPC Link.
