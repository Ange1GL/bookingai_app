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
- `auth`, `home`, `customers` (buscar, alta manual y lista negra con confirmación; ver `docs/customers-management.md`), `appointments` (calendario mes/semana, solo lectura), `price-catalog` (CRUD del catálogo de precios con confirmación al eliminar; ver `docs/price-catalog-management.md`) y `assistant` ("Asistente de AI": chat de texto para agendar citas; la voz quedó pospuesta, ver `docs/assistant-chat.md`)

## Conventions
- Components never inline their template: use `templateUrl` with a sibling `<name>.component.html` (root: `app.html`)
- Never run `npm audit fix --force`; keep all `@angular/*` runtime packages on the same version (they peer-pin each other)
- `overrides` in `package.json` are temporary security patches; document them in `docs/security-remediation-plan.md`
- `environment.apiBaseUrl` is the full host (scheme+host+port) only; services write the full path `${environment.apiBaseUrl}/api/v1/...`. No `constants` folder
- CI: `.github/workflows/security-audit.yml`; Dependabot: `.github/dependabot.yml`
- Utility scripts are Node `.mjs` files (cross-platform)

## Docs
- `docs/security-remediation-plan.md`, `docs/price-catalog-management.md`, `docs/frontend-structure.md`, `docs/appointments-calendar.md`, `docs/assistant-chat.md`, `docs/customers-management.md`
- Backend: `backend/docs/customer-blacklist.md` (lista negra por bloqueo directo, única vía: `POST /customers/{id}/blacklist` con motivo opcional ≤ 250, idempotente; `DELETE /customers/{id}/blacklist` para quitar; un cliente bloqueado no puede reservar y sus citas futuras se cancelan; sin umbrales ni no-shows)
- Backend: `backend/docs/customer-list.md` (`GET /api/v1/customers` paginado por tenant, filtros `name`/`phone`, `size` máx. 50)
- Backend: `backend/docs/actuator-health.md` (Actuator solo expone `GET /actuator/health` público y sin detalle, para health check de ECS Fargate/ALB; no exponer otros endpoints)
- Backend: `backend/docs/price-catalog.md` (cada cita referencia un servicio del catálogo de precios; el frontend debe enviar `priceCatalogId` al crear citas)
