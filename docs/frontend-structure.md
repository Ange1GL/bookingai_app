# Estructura del frontend (por feature)

Basada en `Ange1GL/template_angular_primeng_21` (`core / feature / shared`), con interceptores funcionales y señales.

```
frontend/src/
  main.ts · index.html · styles.css (Tailwind 4 + tailwindcss-primeui + primeicons)
  environments/environment{,.prod}.ts        # apiBaseUrl = host completo (http://localhost:8082)
  app/
    app.ts · app.config.ts · app.routes.ts
    core/                                    # singletons transversales
      guard/auth.guard.ts
      interceptor/{credentials,loader,error,refresh}.interceptor.ts
      model/{api-error,auth-user}.dto.ts
      service/{session,loader,error-handler}.service.ts
    shared/                                  # UI reutilizable sin lógica de negocio
      components/{loader-screen,brand-logo,auth-card}/   # marca bookingAI y contenedor degradado de auth
      layout/main-layout/
    feature/<name>/                          # un directorio por feature
      <name>.routes.ts                       # rutas lazy
      page/<x>-page/                         # componentes enrutados
      components/<x>/                        # componentes presentacionales
      models/                                # DTOs de la feature
      service/                               # acceso a API de la feature
```

Features actuales: `auth` (login, registro) y `home` (página protegida mínima).

## Tema y tipografía
- Preset PrimeNG: `VioletPreset` (Aura con `semantic.primary` → paleta `violet`) en `app.config.ts`; modo oscuro con `.app-dark`.
- Fuente: Montserrat (Google Fonts en `index.html`), expuesta como `--font-sans` en `styles.css`; los componentes PrimeNG la heredan del `body`.
- Clases Tailwind de color: `primary-[50-950]` (vía `tailwindcss-primeui`).
- Login: el usuario se valida como `required` + sin espacios (`USERNAME_PATTERN`); el backend no impone formato de username, y el email solo existe en el registro.

## Regla de dependencias
`feature → core | shared`. `shared → core`. **Nunca** `feature → feature` ni `core → feature`.
Por eso el estado de sesión (`SessionService`) vive en `core/`, ya que lo usan el guard y los interceptores; `feature/auth` solo hace login/registro y lo alimenta.

Alias TypeScript: `@/*` → `src/app/*` (p. ej. `@/core/service/session.service`).

## Autenticación (backend Spring, cookies HttpOnly)
- No hay JWT en JavaScript: `credentialsInterceptor` añade `withCredentials` a `/api/v1/**`.
- CSRF `csrf.spa()`: Angular reenvía `XSRF-TOKEN` como `X-XSRF-TOKEN` (**solo aplica a mismo origen**: Angular no lo envía a URLs absolutas de otro origen; en dev el backend lo tiene deshabilitado, para prod habrá que servir SPA y API bajo el mismo host o enviar el header manualmente).
- Orden de interceptores: `credentials → loader → error → refresh`. `refresh` es el más interno: ante 401 llama una sola vez a `/auth/refresh` (compartido entre peticiones concurrentes) y reintenta; si falla, `SessionService.expire()` redirige a login. `error` solo muestra toasts de errores no recuperados.
- Tras recargar la página no hay estado: `authGuard` restaura la sesión con `/auth/refresh` (devuelve el usuario).

## Cómo añadir una feature
1. Crear `feature/<name>/` con `routes`, `page/`, `components/`, `models/`, `service/`.
2. Registrar la ruta lazy en `app.routes.ts` (dentro del bloque protegido por `authGuard` si requiere sesión).
3. En el service, ruta completa: `${environment.apiBaseUrl}/api/v1/<recurso>`; DTOs reflejando los records del backend.
4. Specs con Vitest + `HttpTestingController` junto al archivo.

## Desarrollo
```bash
npm start            # ng serve; API en http://localhost:8082
npm run build
npx ng test --watch=false
npm run audit
```
