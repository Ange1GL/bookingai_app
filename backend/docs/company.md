# Company (negocio)

Datos mínimos del negocio de citas de un usuario: `name` (≤ 100 caracteres). `User` es el dueño del tenant y tiene
**un solo negocio**: relación uno a uno, la tabla `company` usa `user_id` como clave primaria (y FK a `user_app`),
sin id propio. Por eso el "id del negocio" que ve el agente de WhatsApp (`businessId`) es el id del dueño.
Los clientes de WhatsApp lo buscan por nombre del negocio o por nombre del dueño y se agenda bajo ese `userId`
(ver `.plans/whatsapp-customer-booking-flow.md`).

## Endpoints (autenticados con JWT)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/companies` | Crea la compañía del usuario autenticado. El dueño nunca viene del cuerpo. |
| GET | `/api/v1/companies/me` | Devuelve la compañía del usuario autenticado. |

Crear:

```json
POST /api/v1/companies
{ "name": "Barbería El Güero" }

201 { "name": "Barbería El Güero" }
```

Errores: `400` nombre vacío o > 100; `409` el usuario ya tiene compañía; `404` en `GET /me` si aún no tiene.

## Capas

- Dominio: `Company(userId, name)` (valida y recorta el nombre), `CompanyAlreadyExistsException`, `CompanyNotFoundException`.
- Casos de uso: `CreateCompanyUseCase` y `FindCompanyUseCase`:
  - `findByOwner(userId)`.
  - `searchByName(texto)` y `searchByOwnerName(texto)`: coincidencia **parcial** (contains) y **sin distinguir
    mayúsculas**, máx. 10, solo dueños activos, orden alfabético; vacío si el texto está en blanco. Devuelven
    `CompanySearchResult(ownerUserId, companyName, ownerName)`; si varios dueños comparten nombre, devuelven todos
    para que el agente pida al cliente elegir (distinguiendo por nombre del negocio). El texto se pasa con
    `Containing`, que escapa `%` y `_`. No ignora acentos (`guero` no encuentra `Güero`).
- Persistencia: `V20__create_company.sql` y `V21__company_one_to_one_with_user.sql` (PK = `user_id`); `CompanyEntity`
  con `@OneToOne` de solo lectura a `UserEntity` para buscar por nombre del dueño (las búsquedas usan `EntityGraph`
  para traer al dueño en la misma consulta). Un `save` repetido del mismo usuario sobrescribe el nombre, no duplica;
  la regla de "una compañía por usuario" al crear la hace `CreateCompanyService`.
