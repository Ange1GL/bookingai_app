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

## Conventions
- Never run `npm audit fix --force`; keep all `@angular/*` runtime packages on the same version (they peer-pin each other)
- `overrides` in `package.json` are temporary security patches; document them in `docs/security-remediation-plan.md`
- `environment.apiBaseUrl` is the full host (scheme+host+port) only; services write the full path `${environment.apiBaseUrl}/api/v1/...`. No `constants` folder
- CI: `.github/workflows/security-audit.yml`; Dependabot: `.github/dependabot.yml`
- Utility scripts are Node `.mjs` files (cross-platform)

## Docs
- `docs/security-remediation-plan.md`, `docs/frontend-structure.md`
