# Company (negocio)

Datos mínimos de un negocio: `name` (≤ 100 caracteres) y `userId` (dueño). **Una compañía por usuario**
(`uk_company_user_id`). Sirve de puente para que los clientes de WhatsApp encuentren el negocio por nombre y se agende
bajo el `userId` del dueño (ver `.plans/whatsapp-customer-booking-flow.md`).

## Endpoints (autenticados con JWT)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/companies` | Crea la compañía del usuario autenticado. El dueño nunca viene del cuerpo. |
| GET | `/api/v1/companies/me` | Devuelve la compañía del usuario autenticado. |

Crear:

```json
POST /api/v1/companies
{ "name": "Barbería El Güero" }

201 { "id": 1, "name": "Barbería El Güero" }
```

Errores: `400` nombre vacío o > 100; `409` el usuario ya tiene compañía; `404` en `GET /me` si aún no tiene.

## Capas

- Dominio: `Company` (valida y recorta el nombre), `CompanyAlreadyExistsException`, `CompanyNotFoundException`.
- Casos de uso: `CreateCompanyUseCase`, `FindCompanyUseCase` (`findByOwner`, `searchByName`: coincidencia parcial sin
  distinguir mayúsculas, máx. 10 resultados, vacío si el texto está en blanco).
- Persistencia: tabla `company` (`V20__create_company.sql`), adapter `JpaCompanyRepositoryAdapter` que traduce la
  violación de `uk_company_user_id` a `CompanyAlreadyExistsException` (peticiones simultáneas).
