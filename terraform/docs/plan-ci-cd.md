# Plan: adaptar `.github/` y `.aws/` al entorno creado con Terraform (pendiente de aprobación)

Hoy los dos workflows de GitHub Actions están **comentados por completo** y apuntan al QA manual
(`aglacita-qa-*`). El JSON de la tarea (`.aws/task-definition-backend.json`) es una copia de la tarea
manual. Este plan los alinea con lo que Terraform creó (`aglacita-tf-qa-*`). Estado: **paso 1 hecho** (JSON y variables de los workflows, aún comentados). El `ignore_changes` de A1 (paso 3) **ya está aplicado en `ecs.tf`**; antes NO lo estaba: con él, `terraform apply` registraría la tarea con la imagen nueva pero el servicio seguiría en la revisión anterior (`:initial`, inexistente); solo debe añadirse cuando el CI sea quien despliega.

## Qué cambia, archivo por archivo

### `.aws/task-definition-backend.json` (espejo de `aws_ecs_task_definition.api`)

| Campo | Hoy (QA manual) | Nuevo (Terraform) |
|---|---|---|
| `family` | `aglacita-task-qa-useast` | `aglacita-tf-qa-api` |
| `image` | digest de `aglacita_qa_ecr_useast` | `707256068332.dkr.ecr.us-east-1.amazonaws.com/aglacita-tf-qa-api:PLACEHOLDER` (la acción lo reemplaza) |
| `cpu` / `memory` | `1024` / `2048` | `256` / `512` (valores por defecto de Terraform) |
| `executionRoleArn` | `ecsTaskExecutionRole` | `aglacita-tf-qa-api-execution` (rol con permiso solo sobre `/aglacita-tf/qa/*`) |
| `secrets[].valueFrom` | `parameter/DB_URL`, etc. | `parameter/aglacita-tf/qa/DB_URL`, etc. |
| `environment` | `HOST_CORS`, `SPRING_PROFILES_ACTIVE` fijos | vacío: ambos ya viven en SSM (`/aglacita-tf/qa/HOST_CORS` sale del dominio de CloudFront) |
| `logConfiguration` | grupo `/ecs/aglacita-task-qa-useast`, `awslogs-create-group` | grupo `/ecs/aglacita-tf-qa-api`, prefijo `api`, **sin** `create-group` (lo crea Terraform con retención) |
| `healthCheck` | no tiene | **ninguno por ahora**: la imagen no trae `curl` y el health check de `ecs.tf` está apagado (`api_container_healthcheck = false`). Al instalar `curl` en el Dockerfile, vuelve a activarse en los dos sitios |
| `portMappings` | con `name` y `appProtocol` | solo `containerPort: 8082`, como en Terraform |

Secretos: se añade `WHATSAPP_VERIFY_TOKEN` (la app lo exige y el JSON manual no lo tenía) y `HOST_CORS`.
`VAPID_*` y `SYSTEM_USER_EMPLOYEE_META` **no están en el Terraform**; en `application.yaml` tienen valor
por defecto vacío, así que la app arranca, pero las notificaciones push quedarían desactivadas. ¿Las quieres?

### `.github/workflows/backend.yaml`

| Variable | Hoy | Nuevo |
|---|---|---|
| `ECR_REPOSITORY` | `aglacita_qa_ecr_useast` | `aglacita-tf-qa-api` |
| `ECS_CLUSTER` | `aglacita-clusterecr-qa-useast` | `aglacita-tf-qa-cluster` |
| `ECS_SERVICE` | `aglacita-qa-api` | `aglacita-tf-qa-api` |
| `role-to-assume` | `aglacita-backend-deploy` | `aglacita-tf-qa-backend-deploy` |
| `docker build` | sin flags | `--platform linux/amd64 --provenance=false --sbom=false` (ECR inmutable) |
| `IMAGE_TAG` | `github.sha` | hash corto, como el script local |

### `.github/workflows/frontend.yaml`

| Variable | Hoy | Nuevo |
|---|---|---|
| `S3_BUCKET` | `aglacita-qa-front` | `aglacita-tf-qa-front-707256068332` |
| `role-to-assume` | `aglacita-frontend-deploy` | `aglacita-tf-qa-frontend-deploy` |
| `CLOUDFRONT_DISTRIBUTION_ID` (variable del repo en GitHub) | la del QA manual | `terraform output front_distribution_id` (**cambia cada vez que se recrea el entorno**) |

`BUILD_DIR` (`frontend/dist/diamond-ng/browser`) ya es correcto.

## Tres conflictos que hay que resolver antes de activar los workflows

1. **Dos dueños de la tarea de ECS.** Terraform fija el servicio a su propia revisión de la tarea. Si el CI
   registra revisiones nuevas, el siguiente `terraform apply` las revierte. El state es local, así que el CI
   **no puede ejecutar Terraform**.
2. **Los roles de despliegue no existen para el entorno `aglacita-tf`.** Los roles `aglacita-*-deploy`
   existentes tienen permisos sobre el QA manual. Hacen falta roles nuevos con confianza OIDC solo para
   `Ange1GL/bookingai_app` en `main`, y permisos mínimos sobre los recursos `aglacita-tf-*`.
3. **`aws_s3_object.index` pisa al build de Angular.** Terraform sube una página de prueba como
   `index.html`; el workflow la borra con `--delete` y sube la real, pero el siguiente `terraform apply` la
   vuelve a escribir encima.

## Decisiones tuyas

**A. ¿Quién despliega la API?**
- **A1 (recomendada):** el CI registra revisiones y actualiza el servicio. En `aws_ecs_service.api` se añade
  `lifecycle { ignore_changes = [task_definition] }`: Terraform deja de revertir. El JSON queda como la fuente
  de verdad de la tarea *para el CI*, y hay que mantenerlo sincronizado a mano con `ecs.tf`.
- **A2:** el CI solo construye y sube la imagen; el despliegue sigue siendo `terraform apply` local con la
  etiqueta nueva. Más simple y sin conflicto, pero no es un despliegue continuo real. Después de la lección 7
  (state remoto) podría ejecutarse Terraform desde el CI.

**B (decidido: se reutilizan los roles existentes `aglacita-backend-deploy` y `aglacita-frontend-deploy`; hay que ampliar sus políticas en IAM a mano y los workflows usan esos nombres). Opción original: ¿creo los roles de despliegue con Terraform?** (nuevo `github_actions.tf`: 2 roles OIDC con confianza en
tu repo, más políticas mínimas; el proveedor OIDC de GitHub ya existe en la cuenta, se referencia con `data`).
Tu usuario `angel` tendría que poder crearlos: su política actual ya permite `iam:*Role*` sobre
`aglacita-tf-*`, pero necesita además `iam:GetOpenIDConnectProvider` y `iam:CreatePolicy` o políticas inline.

**C. ¿Reactivo los workflows (los descomento)?** Quedarían activos en cada push a `main`. Recomiendo dejarlos
comentados hasta que A, B y el conflicto 3 estén listos, y activarlos en un commit aparte.

**D. `aws_s3_object.index`:** quitarlo de Terraform (y su `site/index.html`) cuando actives el frontend, o
añadir `lifecycle { ignore_changes = [source, etag] }`.

## Orden propuesto, un commit por paso
1. Actualizar el JSON y las dos variables de entorno de los workflows (sin descomentar).
2. `github_actions.tf` con los roles (plan y apply juntos, solo si apruebas B).
3. `ignore_changes` en el servicio (si eliges A1) y retirar el `aws_s3_object.index` (D).
4. Descomentar los workflows y probar con `workflow_dispatch`, no con un push.
5. Actualizar `README.md`, `CLAUDE.md` y `docs/` en cada commit.

## Riesgos
- Los roles OIDC mal acotados permitirían a otro repositorio desplegar: la confianza se limita por
  `repo:Ange1GL/bookingai_app:ref:refs/heads/main`.
- No puedo probar los workflows localmente; la primera ejecución real sería en GitHub.
- La lista de permisos de IAM del paso B no se probaría hasta el primer despliegue.
