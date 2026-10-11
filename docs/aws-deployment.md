# Despliegue del frontend (S3 + CloudFront)

Workflow: `.github/workflows/frontend.yaml`. Se ejecuta en cada push a `main` que toque `frontend/**` (o a mano
con `workflow_dispatch`).

## Arquitectura

- CloudFront sirve la SPA desde el bucket privado `aglacita-qa-front` (acceso por OAC) y enruta `/api/*` a
  API Gateway -> ECS Fargate. Navegador y API comparten origen, por eso `environment.prod.ts` usa
  `apiBaseUrl: ''` (rutas relativas `/api/v1/...`) y no hace falta CORS entre ambos.

## Flujo del pipeline

1. `npm ci` y `npm run build -- --configuration production` (salida: `frontend/dist/diamond-ng/browser`). Sin tests por ahora.
2. Credenciales temporales por OIDC asumiendo el rol `aglacita-frontend-deploy` (sin llaves guardadas).
3. `aws s3 sync ... --delete --exclude index.html` con `Cache-Control: public, max-age=31536000, immutable`
   (los archivos de Angular llevan hash en el nombre).
4. `index.html` se sube **al final** con `Cache-Control: no-cache`, para que nadie vea un `index.html` que
   apunte a archivos aun no subidos.
5. Invalidación de CloudFront `/*` y espera a que termine (`invalidation-completed`), para que el cambio sea
   visible al terminar el job.

## Configuración requerida

| Dónde | Qué |
|---|---|
| GitHub: Settings > Secrets and variables > Actions > **Variables** | `CLOUDFRONT_DISTRIBUTION_ID` (ID `E...` de la distribución). Es un identificador, no un secreto |
| IAM | Rol `aglacita-frontend-deploy` con trust policy OIDC de GitHub limitada a `repo:Ange1GL/bookingai_app:ref:refs/heads/main` (usa el mismo formato de `sub` que el rol del backend) |
| IAM (policy del rol) | `s3:ListBucket` en el bucket; `s3:PutObject` y `s3:DeleteObject` en `bucket/*`; `cloudfront:CreateInvalidation` en la distribución |
| CloudFront | Objeto raíz `index.html`; errores 403 y 404 -> `/index.html` con código 200 (rutas del router de Angular); comportamiento `/api/*` hacia API Gateway con cookies, header `X-XSRF-TOKEN` y métodos `POST/PUT/DELETE` |

## Notas

- `--delete` borra archivos viejos del bucket: una pestaña abierta con la versión anterior puede fallar al cargar un chunk
  que ya no existe; recargar lo resuelve.
- Rollback: re-ejecutar el workflow desde un commit anterior (`workflow_dispatch` sobre esa referencia, o revertir y push).
- Si el deploy falla en `AssumeRoleWithWebIdentity`, revisar que el `sub` de la trust policy coincida con el del rol del backend.
