# Administración de usuarios (frontend)

Pantallas para el alta manual de usuarios y la recuperación de contraseña. El backend está descrito en
[`backend/docs/manual-onboarding-and-password-recovery.md`](../backend/docs/manual-onboarding-and-password-recovery.md).

## Rutas

| Ruta | Pantalla | Acceso |
|---|---|---|
| `/users` | Listado con búsqueda (`q`), filtro Todos/Activos/Inactivos y paginación; botón **Nuevo** | `USER_READ` (botones de escritura: `USER_WRITE`) |
| `/users/recovery-requests` | Bandeja de solicitudes de recuperación pendientes | `USER_READ` (botón **Generar código**: `USER_WRITE`) |
| `/auth/password-recovery` | Pública: el usuario escribe solo su nombre de usuario | sin sesión |

`ADMIN_ALL` pasa siempre cualquier requisito de permiso.

## Seguridad en el cliente

- `authGuard` protege el layout principal; `permissionGuard(...permisos)` (`core/guard/permission.guard.ts`) se aplica
  encima en `/users` y exige alguno de los permisos. Si falta, redirige a `/home` con un aviso.
- `SessionService.hasAnyPermission()` y la directiva `*appHasPermission` (`shared/directives`) ocultan botones y
  entradas de menú. El menú (`main-layout`) y los accesos de `home` se filtran con el mismo criterio.
- Los permisos vienen en `permissions` de la respuesta de login/refresh/create-password
  (`AuthSuccessResponse.permissions`, nuevo). **Solo es una ayuda de UI**: el backend sigue validando cada llamada con
  `@PreAuthorize`, así que manipular el cliente no da acceso.
- Los permisos se leen al abrir sesión/refrescar: si cambian los roles de alguien, verá el menú actualizado en su
  siguiente refresh o login.
- `PERMISSIONS` (`core/security/permissions.ts`) refleja `Authorities` del backend; añade ahí los nombres nuevos.

## Flujo del código de acceso

1. **Nuevo usuario** (`UserFormDialogComponent`): usuario, nombre, apellidos, teléfono (10 dígitos, sin prefijo de país), correo opcional y roles (se cargan de `GET /roles?active=true` al abrir el diálogo).
2. Al guardar se abre `AccessCodeDialogComponent` con el usuario, el código y el `shareMessage`, cada uno con su botón
   **Copiar** (`CopyButtonComponent`, con respaldo a `execCommand` si no hay Clipboard API). El código se muestra una
   sola vez; si se pierde, **Nuevo código** en el listado lo regenera.
3. **Nuevo código** / **Generar código** pasan por `ResetPasswordFlowComponent` (confirmación → `POST
   /users/{id}/reset-password` → diálogo del código). Borra la contraseña actual y cierra las sesiones del usuario.
4. El usuario canjea el código en `/auth/create-password`, en dos pasos: primero verifica usuario y código
   (`POST /auth/verify-code`) y solo entonces elige su contraseña.

## Recuperación de contraseña

El usuario entra a **¿Olvidaste tu contraseña?** (enlace en el login), escribe su usuario y ve siempre el mismo mensaje
del backend, exista o no la cuenta. El administrador revisa la bandeja, verifica a la persona y genera el código
(recomendación: enviarlo al teléfono registrado).

## Estructura

`feature/users` (`pages`, `components`, `models`, `service`, `users.routes.ts`) y `feature/auth/page/password-recovery-page`.
Comparten: `core/service/clipboard.service.ts`, `shared/components/copy-button`, `shared/directives/has-permission.directive.ts`.
