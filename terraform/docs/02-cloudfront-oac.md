# Lección 2: CloudFront + OAC delante del bucket privado

Hasta ahora nadie puede leer el bucket. Aquí pones CloudFront delante: es el **único** con permiso de
lectura. El código está en [`../envs/qa/cloudfront.tf`](../envs/qa/cloudfront.tf).

```
navegador ──HTTPS──► CloudFront ──(firma OAC)──► bucket S3 privado
                       │  headers de seguridad
                       │  403/404 → index.html (SPA)
```

## Conceptos de Terraform

### Varios archivos, un solo proyecto

Terraform junta **todos los `.tf` de la carpeta**. Por eso la lección 2 vive en `cloudfront.tf` y
`main.tf` (S3) no se toca. Puede referenciar `aws_s3_bucket.front` sin importar nada.

### `data` para políticas administradas

```hcl
data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}
```

AWS ya trae políticas listas. Se buscan por nombre y se usa `.id`, en vez de pegar un id opaco.

### `dynamic` y `for_each`

```hcl
dynamic "custom_error_response" {
  for_each = toset([403, 404])
  content { error_code = custom_error_response.value ... }
}
```

Repite un bloque por cada elemento de una lista. Aquí genera dos bloques idénticos que solo cambian el
código. Ojo: úsalo cuando la repetición sea real; para dos bloques distintos, escríbelos a mano.

### `jsonencode` / `aws_iam_policy_document`

Las políticas son JSON. En vez de escribir el texto, `data "aws_iam_policy_document"` te deja declararlo
con bloques HCL (validados por `terraform validate`) y expone `.json`.

### Dependencias entre tres recursos

`aws_s3_bucket_policy` necesita el ARN de la distribución, y la distribución necesita el bucket. No hay
ciclo porque la distribución **no** depende de la política. Terraform los ordena solo:
bucket → distribución → política.

`depends_on = [aws_s3_bucket_public_access_block.front]` fuerza un orden que Terraform no puede deducir:
S3 rechaza cambiar el bloqueo público y la política a la vez.

### `path.module` y `filemd5`

`aws_s3_object.index` sube `site/index.html`. `etag = filemd5(...)` hace que, si cambias el archivo, el
siguiente `plan` muestre `~` y lo vuelva a subir.

## Seguridad: qué hace cada pieza

| Pieza | Medida |
|---|---|
| OAC (`sigv4`, `always`) | CloudFront firma sus peticiones a S3; el bucket sigue sin acceso público |
| Política del bucket con `AWS:SourceArn` | Solo **esta** distribución lee; ni otra distribución de tu cuenta |
| `redirect-to-https` | El navegador siempre usa HTTPS |
| `Managed-SecurityHeadersPolicy` | HSTS, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` |
| `custom_error_response` 403/404 → `index.html` | Recargar `/dashboard` no devuelve un error de S3 |

Mejoras de la sección 10 de tu documento aplicadas aquí: páginas de error de SPA y response headers.
No incluye WAF ni dominio propio (siguen en "convenientes" y "producción").

> Nota sobre costo: el plan "Free" de CloudFront de tu QA manual es un plan de precio plano que se
> contrata aparte. Esta distribución usa el precio normal por uso; con tráfico de QA el costo es
> prácticamente cero (la capa gratuita incluye 1 TB y 10 M de peticiones al mes).

## Permisos de IAM necesarios

Hasta ahora `angel` solo tiene S3. Esta lección usa CloudFront y, por las etiquetas de `default_tags`,
también necesita permisos de etiquetas en los **objetos** de S3. Crea una política nueva
(`terraform-aglacita-tf-cloudfront`) con:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "TerraformCloudFront",
			"Effect": "Allow",
			"Action": [
				"cloudfront:CreateDistribution",
				"cloudfront:CreateDistributionWithTags",
				"cloudfront:GetDistribution",
				"cloudfront:GetDistributionConfig",
				"cloudfront:UpdateDistribution",
				"cloudfront:DeleteDistribution",
				"cloudfront:TagResource",
				"cloudfront:UntagResource",
				"cloudfront:ListTagsForResource",
				"cloudfront:CreateOriginAccessControl",
				"cloudfront:GetOriginAccessControl",
				"cloudfront:UpdateOriginAccessControl",
				"cloudfront:DeleteOriginAccessControl",
				"cloudfront:ListCachePolicies",
				"cloudfront:GetCachePolicy",
				"cloudfront:ListResponseHeadersPolicies",
				"cloudfront:GetResponseHeadersPolicy",
				"cloudfront:CreateInvalidation"
			],
			"Resource": "*"
		}
	]
}
```

Y en la política de S3 existente, agrega al bloque `DeployFrontObjects` estas tres acciones:

```
"s3:GetObjectTagging", "s3:PutObjectTagging", "s3:DeleteObjectTagging"
```

Por qué `Resource: "*"`: los ids de CloudFront no existen hasta crearlos, así que no se pueden fijar de
antemano. Es la limitación habitual; se compensa limitando las **acciones**.

Si Terraform pide otro permiso, el error dice cuál (`not authorized to perform: ...`): se agrega a la lista.

## Ejecutar

```bash
cd terraform/envs/qa
terraform plan
```

Esperado: `Plan: 7 to add, 0 to change, 0 to destroy.`

Si ves un aviso `AWS resource not found during refresh`, es **deriva**: el bucket de la lección 1 ya no
existe en AWS (lo borraste), Terraform lo detecta y lo recreará. El state no miente, solo estaba
desactualizado.

```bash
terraform apply     # escribe yes
```

**Es normal que CloudFront tarde de 3 a 10 minutos** en `Creating...`: propaga la configuración a todas
sus ubicaciones. No es el cuelgue por permisos de la lección 1; allí tardó minutos en algo que debía
tardar segundos. Aquí `Still creating...` es lo esperado.

## Qué observar

1. Outputs: `front_url` y `front_distribution_id`. Abre `front_url`: ves la página de prueba por HTTPS.
2. Abre `http://<dominio>.cloudfront.net` (sin la `s`): redirige a HTTPS.
3. Abre `https://<dominio>/cualquier-ruta`: devuelve `index.html` con 200 (la regla de SPA).
4. Intenta leer el bucket directo, por su URL de S3: **Access Denied**. Solo CloudFront puede.
5. En las cabeceras de respuesta (`curl -I`), busca `strict-transport-security` y `x-content-type-options`.

## Limpiar

```bash
terraform destroy
```

CloudFront tarda varios minutos en borrarse (primero se deshabilita). Es normal.

## Ejercicios

1. Cambia el texto de `site/index.html` y ejecuta `plan`: solo aparece `~ aws_s3_object.index`. Aplica y
   recarga: puede que veas la versión vieja unos minutos. Eso es la **caché** y por eso en la lección de
   despliegue invalidaremos `/index.html`.
2. Cambia `price_class` a `PriceClass_All`: el `plan` muestra `~` in situ, sin recrear.
3. Pregunta: ¿qué pasa si cambias el `name` del OAC? (Pista: `plan` muestra `-/+`, reemplazo.)

Siguiente: Lección 3, red y security groups.
