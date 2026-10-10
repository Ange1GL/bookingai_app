# Gestión de clientes (frontend)

Ruta `/customers` (feature `customers`, lazy, dentro del layout protegido). Permite buscar clientes, agregar uno manualmente y bloquear/desbloquear con confirmación.

## Contrato con el backend (`CustomerController`)
| Acción | Endpoint | Notas |
|--------|----------|-------|
| Listar / buscar | `GET /api/v1/customers?name&phone&blacklisted&page&size` | `size` fijo en 10 (`PAGE_SIZE`, máx. backend 50). |
| Crear | `POST /api/v1/customers` `{ name, phone }` | Idempotente por teléfono: responde 200 con el cliente existente si ya estaba. |
| Bloquear | `POST /api/v1/customers/{id}/blacklist` `{ reason? }` | `reason` ≤ 250; se omite si está vacío. Cancela citas futuras. |
| Desbloquear | `DELETE /api/v1/customers/{id}/blacklist` | |

Una sola caja de búsqueda: si el texto son solo dígitos/`+ ( ) -`/espacios se envía como `phone`; en otro caso como `name` (`toSearchParams`). El filtro "Lista negra" envía `blacklisted=true`.

## Estructura
```
feature/customers/
  customers.routes.ts
  page/customers-page/           # orquesta estado, recurso y mutaciones
  components/
    customer-card/               # fila presentacional (inputs/outputs, sin servicios)
    customer-filters/            # búsqueda + filtro lista negra
    customer-form-dialog/        # alta manual (reactive form, valida nombre ≤100, teléfono ≤20)
    blacklist-dialog/            # confirmación + motivo opcional (usa app-confirm-dialog)
  models/ · service/customers.service.ts
shared/components/
  search-input/                  # input con debounce 300 ms, emite valor recortado
  confirm-dialog/                # modal de confirmación reutilizable con contenido proyectable
  pager/                         # paginador propio (píldoras; "x / y" en móvil, números con … en ≥sm); lógica en utils/pagination.util.ts
```

## Rendimiento y limpieza
- Se usan tarjetas (no tabla) por ser mobile-first, y un paginador propio en lugar de `p-paginator` para mantener el estilo; se deshabilita mientras carga y siempre se muestra si hay resultados (con flechas inactivas si solo hay una página).
- Todos los componentes `OnPush`, señales y `@for ... track id`.
- Lista con `rxResource` cuyo `params` es un único `query`; cambiar filtro reinicia `page` a 0. `linkedSignal` conserva la última página mientras carga la siguiente (sin parpadeo de skeleton).
- Búsqueda con `debounceTime` + `distinctUntilChanged`; `rxResource` cancela la petición anterior al cambiar los parámetros.
- Suscripciones manuales con `takeUntilDestroyed`; `finalize` apaga los estados de carga. Las llamadas usan `SKIP_LOADER` porque pintan su propio estado.
- Mientras una mutación está en curso se bloquean los diálogos y se ignoran confirmaciones duplicadas (`busyId`).
- Los errores HTTP ya los muestra `errorInterceptor` como toast; los diálogos permanecen abiertos para reintentar.
