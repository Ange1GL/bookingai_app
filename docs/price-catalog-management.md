# Catálogo de precios (frontend)

Pantalla `/price-catalog` (menú "Precios") para que el administrador del negocio gestione los servicios que ofrece y su precio. Backend: `backend/docs/price-catalog.md`.

## Funcionalidad
- **Consultar**: lista de servicios activos del usuario (`GET /api/v1/price-catalog`), con skeleton, estado vacío y reintento si falla.
- **Crear / editar**: `app-price-service-form-dialog` (mismo diálogo; modo editar cuando recibe `service`). Campos: nombre (obligatorio, ≤ 100) y precio entero MXN (1 – 9 999 999). `POST` / `PUT /{id}`.
- **Eliminar**: botón de papelera → `app-confirm-dialog` (severidad `danger`) → `DELETE /{id}`. Es baja lógica: las citas existentes siguen mostrando el servicio, pero ya no se puede asignar a citas nuevas.
- Errores del backend (409 nombre duplicado, 404, etc.) los muestra `errorInterceptor` como toast; el diálogo queda abierto para reintentar.

## Estructura (`src/app/feature/price-catalog`)
- `page/price-catalog-page`: orquesta lista (`rxResource`), diálogos y mutaciones.
- `components/price-service-card`, `components/price-service-form-dialog`.
- `service/price-catalog.service.ts` (+ spec), `models/`.
