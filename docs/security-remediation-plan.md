# Plan de remediación de vulnerabilidades

Fecha: 2026-10-04 · Alcance: `frontend/` (npm) y `backend/` (Maven)
Estado: **Aprobado. Fases 0–4 del frontend ejecutadas en la rama `fix/security-npm` (0 vulnerabilidades). Fase 5 (backend) pendiente.**

Notas de ejecución: el fix de `piscina` (crítica) requeriría `@angular/build@22`; se resolvió con `overrides.piscina ^5.3.2` (retirar cuando `@angular/build` 21 lo incluya). El repo no tiene specs, por lo que `ng test` aún no corre ninguno.

## 1. Diagnóstico: por qué "antes estaba limpio"

Hay dos causas distintas:

### Causa A — Cambios sin commitear que empeoraron todo (42 vulns)
El working tree tiene `package.json` y `package-lock.json` modificados. Alguien (probablemente
`npm audit fix --force`) **bajó** versiones:

| Paquete | Commit (HEAD) | Working tree |
|---|---|---|
| `@angular-devkit/build-angular` | ^21.2.24 | ^19.2.27 |
| `karma` | ~6.4.0 | ^4.0.0 |
| `karma-jasmine` | ~5.1.0 | ^4.0.2 |
| `karma-jasmine-html-reporter` | ~2.1.0 | ^1.7.0 |

Karma 4 arrastra `socket.io 2.x`, `ws 5`, `xmlhttprequest-ssl 1.x`, `tmp`, `useragent`
(7 críticas). `npm audit` sugiere esos "fixes" porque es un resolvedor ciego: son retrocesos
de 2 mayores, incompatibles con Angular 21. **No volver a usar `npm audit fix --force`.**

Resultado medido: working tree = **42 vulns** (7 críticas) · HEAD = **18 vulns** (3 críticas).

### Causa B — HEAD ya no está limpio (18 vulns)
El commit `93d5a30` dejó 0 en su momento; desde entonces se publicaron advisories nuevos. Con
el lockfile de HEAD quedan:

- **Raíz principal:** `@angular-devkit/build-angular` (deprecado, arrastra `@angular/build` viejo,
  `piscina`, `webpack-dev-server`, `http-proxy-middleware`, `micromatch`, `braces`, `chokidar`).
- **Karma 6 + plugins:** `karma`, `karma-jasmine`, `karma-jasmine-html-reporter` (→ `braces`, `chokidar`).
- **Directa, arreglable sin breaking:** `@angular/router` (high).
- **Transitivas arreglables con `npm audit fix` normal:** `brace-expansion`, `fast-uri`,
  `http-cache-semantics`, `ip-address`, `serialize-javascript`.

Todas son **devDependencies / tooling de desarrollo** salvo `@angular/router` (runtime). Ninguna
se empaqueta en el build de producción, pero sí afectan la máquina del desarrollador y el CI.

## 2. Estrategia (de menor a mayor impacto)

> Principio: eliminar la causa raíz (tooling obsoleto) en lugar de parchear síntomas.

### Fase 0 — Volver a una base conocida
1. Descartar los cambios sin commitear de `package.json` / `package-lock.json`
   (`git restore frontend/package.json frontend/package-lock.json`). **Requiere tu confirmación**
   porque descarta cambios locales (están en un estado roto de todas formas).
2. `npm ci` y confirmar `npm audit` = 18.

### Fase 1 — Arreglos sin breaking changes (rama `fix/security-npm`)
1. Subir `@angular/*` a la última 21.2.x (incluye el fix de `@angular/router`).
2. `npm audit fix` (**sin** `--force`) para las transitivas.
3. Verificar `npm run build` y `npm test`.
   Esperado: 18 → ~13 (quedan las de build-angular/karma).

### Fase 2 — Eliminar `build-angular` (cierra críticas)
Migrar al builder moderno incluido en Angular 21:
1. En `angular.json`: `@angular-devkit/build-angular:*` → `@angular/build:application`
   (`ng update @angular/cli --name use-application-builder` lo automatiza) y
   `dev-server` → `@angular/build:dev-server`.
2. Quitar `@angular-devkit/build-angular` de `devDependencies`; añadir `@angular/build`.
3. Verificar `ng build` (prod y dev) y `ng serve`.
Esto elimina `piscina`, `webpack-dev-server`, `http-proxy-middleware`, `micromatch`, etc.

### Fase 3 — Reemplazar Karma
Karma está deprecado y es el origen de `braces`/`chokidar`/`socket.io`. Dos opciones:

| Opción | Pros | Contras |
|---|---|---|
| **A. Vitest vía `@angular/build:unit-test` (recomendada)** | Runner por defecto en Angular 21, sin Karma/Chrome, rápido, 0 vulns asociadas | Migrar specs (casi sin cambios: Jasmine → API compatible de `vitest` globals) |
| B. Mantener Karma 6.4 + plugins actuales | Sin migración | Siguen las vulns de `braces`/`chokidar`; sin fix upstream |

Pasos (opción A): cambiar target `test` en `angular.json`, quitar `karma*`, `jasmine-core`,
`@types/jasmine`; añadir `vitest` + `jsdom`; ajustar `tsconfig.spec.json`; correr los specs.

### Fase 4 — Prevención (para que no vuelva a pasar)
1. `overrides` en `package.json` solo como parche temporal y documentado (ya hay `uuid`, `qs`).
2. Script `scripts/audit.mjs` (Node, multiplataforma) que corre `npm audit --audit-level=high`
   y falla si hay high/critical.
3. CI: job `npm audit --omit=dev --audit-level=high` (bloqueante) + `npm audit` completo (informativo).
4. Dependabot/Renovate semanal para `frontend/` y `backend/`.
5. Regla de equipo: nunca `npm audit fix --force`; revisar el diff de `package.json` antes de commitear.

### Fase 5 — Backend (Spring Boot / Maven) — **no auditado todavía**
No se ha revisado `backend/pom.xml`. Pasos:
1. Ejecutar `./mvnw org.owasp:dependency-check-maven:check` (o `mvnw versions:display-dependency-updates`).
2. Subir Spring Boot a la última patch de su línea (hereda fixes de Spring Security, Tomcat, Jackson).
3. Revisar config: secretos fuera del repo (hay `docs_confidencial/` — verificar que no esté versionado
   con credenciales), JWT con secreto por variable de entorno, CORS restrictivo, headers de seguridad.
4. Añadir el plugin dependency-check al CI.

## 3. Resultado esperado

| Etapa | Vulns totales | Críticas |
|---|---|---|
| Hoy (working tree) | 42 | 7 |
| Fase 0 (HEAD) | 18 | 3 |
| Fase 1 | ~13 | ~3 |
| Fase 2 | ~5 | 0 |
| Fase 3 (Vitest) | **0** | 0 |

Las cifras de fases 1–3 son estimadas; se confirmarán con `npm audit` tras cada fase.

## 4. Orden de commits (Conventional Commits)
1. `fix(frontend): restore dependency baseline after bad audit fix` (si aplica)
2. `fix(frontend): update angular and transitive deps for security advisories`
3. `build(frontend): migrate to @angular/build application builder`
4. `test(frontend): migrate karma to vitest`
5. `ci: add dependency audit checks`
6. `docs: document security policy and audit process`

Cada fase en commit separado y verificable (`build` + `test` verdes) para poder revertir.

## 5. Riesgos
- Fase 2/3 tocan `angular.json` y los specs: probar `ng serve` y `ng build --configuration production`.
- PrimeNG 21 y Tailwind 4 no dependen de `build-angular`, riesgo bajo.
- `postcss`/`tailwindcss` ya son directos y están al día.

## CSRF double-submit (cross-origin API)

- Angular's built-in XSRF interceptor does not send `X-XSRF-TOKEN` when the API origin differs from the app origin (`localhost:4200` -> `localhost:8082`), so `core/interceptor/csrf.interceptor.ts` adds the header for mutating requests to `environment.apiBaseUrl`.
- If the `XSRF-TOKEN` cookie is missing, the interceptor first calls `GET /api/v1/auth/csrf` (public, 204) to obtain it.
- Verify with the backend profile `qa` (`SPRING_PROFILES_ACTIVE=qa`); the default `dev` profile disables CSRF.
- Production: the cookie is issued by the API host and is host-only, so JS on a different host cannot read it. Serve front and API under the same origin (reverse proxy `/api`) or set the cookie `Domain` on the CSRF repository.
