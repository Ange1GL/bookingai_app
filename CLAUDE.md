# BookingApp

Monorepo: `backend/` (Spring Boot, Maven, hexagonal) and `frontend/` (Angular 21, PrimeNG 21, Tailwind 4).

## Frontend commands (run in `frontend/`)
- `npm ci` install, `npm start` dev server (API at `environment.apiBaseUrl`, `http://localhost:8082`), `npm run build`, `npx ng test --watch=false` (Vitest), `npm run audit`
- Builder: `@angular/build` (not the deprecated `@angular-devkit/build-angular`); tests via Vitest, no Karma

## Frontend architecture
- Feature-based: `src/app/{core,shared,feature/<name>}`; see `docs/frontend-structure.md`
- Dependency rule: `feature → core|shared`, never feature → feature or core → feature
- Alias `@/*` → `src/app/*`
- Standalone components, signals, `OnPush`, functional interceptors/guards
- Auth uses HttpOnly cookies (no JWT in JS): `withCredentials`, refresh-on-401, CSRF double-submit via `csrfInterceptor` (Angular's built-in XSRF skips cross-origin requests; it seeds the cookie with `GET /api/v1/auth/csrf`)

## Features
- `landing` (página pública en `/`: qué es, ventajas, contacto `lariosacostaa@gmail.com` y CTA a `/auth/login`; páginas públicas `/privacidad` y `/terminos` (textos en `feature/landing/data/privacy-policy.ts` y `terms-of-service.ts`, renderizados por `LegalDocumentComponent`; deben mantenerse veraces con el código); SaaS sin registro público; ver `docs/frontend-structure.md`), `auth`, `home`, `customers` (buscar, alta manual y lista negra con confirmación; ver `docs/customers-management.md`), `appointments` (calendario mes/semana, solo lectura), `price-catalog` (CRUD del catálogo de precios con confirmación al eliminar; ver `docs/price-catalog-management.md`) y `assistant` ("Asistente de AI": chat de texto para agendar citas; la voz quedó pospuesta, ver `docs/assistant-chat.md`)

## Conventions
- Components never inline their template: use `templateUrl` with a sibling `<name>.component.html` (root: `app.html`)
- Never run `npm audit fix --force`; keep all `@angular/*` runtime packages on the same version (they peer-pin each other)
- `overrides` in `package.json` are temporary security patches; document them in `docs/security-remediation-plan.md`
- `environment.apiBaseUrl` is the full host (scheme+host+port) only; services write the full path `${environment.apiBaseUrl}/api/v1/...`. No `constants` folder
- CI/CD: `.github/workflows/backend.yaml` (ECS) y `frontend.yaml` (S3 + CloudFront); audit: `.github/workflows/security-audit.yml`; Dependabot: `.github/dependabot.yml`
- Utility scripts are Node `.mjs` files (cross-platform)

## Docs
- `docs/aws-deployment.md` (CI/CD del frontend a S3 + CloudFront con invalidación de caché; variable `CLOUDFRONT_DISTRIBUTION_ID`), `docs/security-remediation-plan.md`, `docs/price-catalog-management.md`, `docs/frontend-structure.md`, `docs/appointments-calendar.md`, `docs/assistant-chat.md`, `docs/customers-management.md`
- Backend: `backend/docs/customer-blacklist.md` (lista negra por bloqueo directo, única vía: `POST /customers/{id}/blacklist` con motivo opcional ≤ 250, idempotente; `DELETE /customers/{id}/blacklist` para quitar; un cliente bloqueado no puede reservar y sus citas futuras se cancelan; sin umbrales ni no-shows)
- Backend: `backend/docs/company.md` (negocio mínimo `name` + `userId` dueño, una por usuario; `POST /api/v1/companies` y `GET /api/v1/companies/me`; `searchByName` lo usará el flujo de WhatsApp para clientes, plan en `backend/.plans/whatsapp-customer-booking-flow.md`)
- Backend: `backend/docs/customer-list.md` (`GET /api/v1/customers` paginado por tenant, filtros `name`/`phone`, `size` máx. 50)
- Backend: `backend/docs/actuator-health.md` (Actuator solo expone `GET /actuator/health` público y sin detalle, para health check de ECS Fargate/ALB; no exponer otros endpoints)
- Backend: `backend/docs/price-catalog.md` (cada cita referencia un servicio del catálogo de precios; el frontend debe enviar `priceCatalogId` al crear citas)
- Backend: `backend/docs/whatsapp-webhook.md` (webhook de WhatsApp Cloud API en `/api/v1/webhooks/whatsapp`: GET de verificación con `hub.verify_token` y POST firmado con HMAC-SHA256 (`X-Hub-Signature-256`); público en `permitAll` y excluido de CSRF; secretos `WHATSAPP_VERIFY_TOKEN` y `WHATSAPP_APP_SECRET` sin default (Parameter Store en QA); no loguear cuerpos ni textos de clientes; los mensajes de texto van al agente de clientes (demo): `CustomerBookingTools` aparte de `BookingTools`, tenant desde el estado del servidor y teléfono desde `from` vía `ToolContext`, solo reserva; respuesta solo en log hasta implementar el envío; se procesa `@Async` tras responder 200, con idempotencia por `messageId`, 10 msgs/min por teléfono y un turno a la vez por conversación, todo en memoria)
- Backend: `backend/docs/jwt-secret.md` (JWT firmado con HS256 porque es un monolito: un solo secreto base64 ≥ 32 bytes en `JWT_SECRET`, validado al arrancar por `JwtSecretKeys`, sin valor por defecto; en dev desde `.env`, en QA desde Parameter Store (SecureString); el decoder fija HS256 y valida `exp`/`iss`/`aud`; sin PEM ni `certs/`; nunca el secreto en repo, imagen ni logs; volver a RS256 + JWKS solo si otro servicio debe validar tokens)
