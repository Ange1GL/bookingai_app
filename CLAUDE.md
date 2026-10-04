# BookingApp

Monorepo: `backend/` (Spring Boot, Maven) and `frontend/` (Angular 21, PrimeNG, Tailwind 4).

## Frontend commands (run in `frontend/`)
- `npm ci` install, `npm start` dev server, `npm run build` build, `ng test` Vitest, `npm run audit` security audit
- Builder: `@angular/build` (not the deprecated `@angular-devkit/build-angular`); tests via Vitest, no Karma

## Conventions
- Never run `npm audit fix --force`; keep all `@angular/*` runtime packages on the same version (they peer-pin each other)
- `overrides` in `package.json` are temporary security patches; document them in `docs/security-remediation-plan.md`
- CI: `.github/workflows/security-audit.yml`; Dependabot: `.github/dependabot.yml`
- Utility scripts are Node `.mjs` files (cross-platform)

## Docs
- `docs/security-remediation-plan.md`
