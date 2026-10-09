# Catálogo de precios (PriceCatalog)

Cada cita referencia un servicio del catálogo (`appointment.price_catalog_id`, NOT NULL) para saber cuánto cobrar.

## Modelo
- `PriceCatalog` (dominio): `id`, `label`, `price` (>0), `userId`, `active`. Valida con `InvalidFieldException`.
- Cada usuario (administrador de su negocio) tiene su **propio catálogo**; no existen servicios globales. Un usuario nuevo empieza con el catálogo vacío y debe crear su primer servicio antes de agendar. Solo el dueño ve, edita y elimina sus servicios.
- `Appointment` exige un `PriceCatalog` en `createNew`/`reconstitute`.

## Migraciones
- `V9` crea `price_catalog`; `V10` siembra "Corte básico" = 60 (global); `V11` agrega `appointment.price_catalog_id` (backfill a "Corte básico", luego NOT NULL); `V12` agrega `active` y un índice único (usuario, label sin mayúsculas) entre activos; `V13` asigna al usuario 1 el "Corte básico" sembrado (era solo para probar un flujo), dejando de existir filas con `user_id = NULL`; `V14` hace `user_id` NOT NULL (reasigna al usuario 1 cualquier huérfana) y recrea el índice único sin `coalesce`.

## API (`/api/v1/price-catalog`, requiere sesión + CSRF)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Servicios activos del usuario. |
| POST | `/` | Crea un servicio propio → 201. |
| PUT | `/{id}` | Edita nombre y precio de un servicio propio → 200. |
| DELETE | `/{id}` | Baja lógica (`active = false`) → 204. |

Request (POST/PUT): `{ "label": "Corte + barba", "price": 90 }`
Response: `{ "id": 2, "label": "Corte + barba", "price": 90 }`

Errores: 400 validación (`label` vacío, `price <= 0`), 404 no existe/inactivo/de otro usuario, 409 nombre duplicado entre activos (`DuplicatePriceCatalogLabelException`).

## Reglas
- "Eliminar" es baja lógica porque las citas tienen FK al servicio. Un servicio inactivo no se lista ni se puede asignar a citas nuevas, pero las citas existentes lo siguen mostrando.
- La unicidad (usuario + label sin mayúsculas, solo activos) la garantiza el índice `uk_price_catalog_user_label_active` (V12). `existsActiveByLabel` solo da el mensaje temprano; si dos requests simultáneos pasan ese chequeo, `JpaPriceCatalogRepositoryAdapter.save` traduce la violación del índice a `DuplicatePriceCatalogLabelException` (409, nunca 500).
- Editar el precio cambia el monto mostrado en citas existentes (la cita guarda solo la referencia).

## Flujo de citas y agente
- `BookAppointmentCommand` / `CreateAppointmentCommand` reciben `priceCatalogId`; los servicios lo resuelven con `PriceCatalogRepositoryPort.findById(id, userId)` (404 si no existe, está inactivo o es de otro usuario).
- REST de citas: `priceCatalogId` obligatorio; `AppointmentResponse` devuelve `priceCatalogId`, `serviceLabel`, `price`.
- Agente de IA: tool `listPriceCatalog()`; las tools de agendar reciben `priceCatalogId`; el `SYSTEM_PROMPT` obliga a mostrar el catálogo y preguntar el servicio antes de agendar. El agente no gestiona el catálogo (solo lo lee).
