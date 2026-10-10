# Actuator: health check

Dependencia `spring-boot-starter-actuator`, usada únicamente para el health check de AWS ECS Fargate / ALB.

## Qué se expone

| Endpoint | Acceso | Respuesta |
|----------|--------|-----------|
| `GET /actuator/health` | público (sin JWT), `permitAll` en `SecurityConfig` | `{"status":"UP"}` (HTTP 200) o `{"status":"DOWN"}` (HTTP 503) |

Cualquier otro endpoint de Actuator (`env`, `beans`, `metrics`, `info`, ...) **no está expuesto**: la config usa una
allowlist (`management.endpoints.web.exposure.include: health`) y `/actuator/**` no está en `permitAll`.

## Configuración (`application.yaml`)

- `management.endpoints.web.exposure.include: health`
- `management.endpoint.health.show-details: never` y `show-components: never`: no se filtra el estado de la BD ni de otros componentes.

## ECS / ALB

- Target group del ALB: health check path `/actuator/health`, puerto `8082`, success codes `200`.
- Task definition (opcional): `healthCheck` del contenedor con `curl -f http://localhost:8082/actuator/health` (requiere `curl` en la imagen).
- El health incluye el indicador de la base de datos: si PostgreSQL no responde, devuelve `DOWN` (503) y ECS reemplazará la tarea.
- `RateLimitFilter` aplica también a este endpoint (60 req/min por IP); un health check cada 30 s queda muy por debajo.
