# BookingApp — Backend

Sistema de **reservación de citas asistido por un LLM**. Este software esta dedicado para que un usuario pueda gestionar
las citas de sus clientes mediantes lenguaje natural, permitiendo operaciones básicas como crear, cancelar o reagendar 
una cita.

Este repositorio contiene el **backend**: expone la API REST, gestiona la autenticación con JWT y
orquesta al agente de IA.

## Arquitectura

El backend sigue **arquitectura hexagonal (Ports & Adapters)**:

```
domain/         → modelo y puertos (Java puro, sin Spring/JPA)
application/    → casos de uso (@Service), commands y results
infrastructure/ → adaptadores REST (in), persistencia JPA (out), configuración
```

- El backend es *authorization server* y *resource server* a la vez: emite los JWT firmados con
  **HS256** (secreto simétrico `JWT_SECRET`) y los valida con el mismo secreto. Los tokens viajan en cookies `HttpOnly`.
- El agente de IA (DeepSeek vía Spring AI) usa tools (`@Tool` en `BookingTools`) para ejecutar los
  casos de uso de reservas; el LLM decide *qué* llamar, el backend decide *cómo*.
- El schema de PostgreSQL lo gobierna **Flyway** (Hibernate en `ddl-auto: validate`).

La guía completa de capas, convenciones y checklist está en [CLAUDE.md](CLAUDE.md).

## Stack

- **Java 25** / **Spring Boot 4.x**
- **Spring Security** + **JWT HS256** (OAuth2 Resource Server, tokens en cookies `HttpOnly`)
- **JPA / Hibernate** — PostgreSQL
- **Flyway** — migraciones versionadas del schema
- **Spring AI** — DeepSeek Flash (agente de reservas por lenguaje natural con tools) y OpenAI `gpt-4o-mini-transcribe` (dictado por voz)
- **MapStruct** + **Lombok**
- **Maven**

## Prerrequisitos

- JDK 25+
- Maven 3.9+ (o el wrapper `mvnw` incluido)
- PostgreSQL 16+ corriendo localmente (o vía Docker)
- API key de DeepSeek
- API key de OpenAI (solo para el dictado por voz)

## Configuración

### 1. Base de datos

```sql
CREATE DATABASE bookingapp;
```

Solo hay que crear la base vacía: **las tablas las crea Flyway** al arrancar la app, aplicando las
migraciones de `src/main/resources/db/migration/`. No ejecutes DDL a mano — Hibernate está en
`ddl-auto: validate` y no modifica el schema. Ver [docs/flyway-migrations.md](docs/flyway-migrations.md).

### 2. Variables de entorno

Crea un archivo `.env` en la raíz del módulo backend (está en `.gitignore`, nunca lo comitas):

```properties
DB_URL=jdbc:postgresql://localhost:5432/bookingapp
DB_USERNAME=postgres
DB_PASSWORD=tu_password

DEEPSEEK_API_KEY=tu_api_key_deepseek

# Dictado por voz (transcripcion con gpt-4o-mini-transcribe). Ver docs/voice-transcription.md
API_OPENAI_KEY=tu_api_key_openai

# Secreto HS256 del JWT (base64, >= 32 bytes). Ver la sección 3
JWT_SECRET=valor_generado
```

### 3. Secreto del JWT (HS256)

Los JWT se firman y se validan con **HS256** usando un único secreto simétrico en `JWT_SECRET`
(sin valor por defecto: si falta, la app no arranca). Genera uno distinto por ambiente:

```bash
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

(o `openssl rand -base64 32`). Pega el resultado en `.env` en una sola línea, sin comillas. Nunca lo
comitees. En QA se inyecta desde Parameter Store. Detalle, validaciones y rotación en
[docs/jwt-secret.md](docs/jwt-secret.md).

### 4. Rate limiting

Cada IP tiene un límite de peticiones por minuto (token bucket vía Bucket4j + Caffeine), configurable en `application.yaml`:

```yaml
security:
  rate-limit:
    enabled: true
    capacity: 60        # peticiones permitidas por ventana, por IP
    window: 1m
    cache:
      expire-after-access: 5m
      maximum-size: 10000
```

Al exceder el límite, la API responde `429` con el mismo formato de error que el resto de la API y un header `Retry-After`. Detalle completo en [docs/rate-limiting.md](docs/rate-limiting.md).

## Comandos

```bash
# Compilar
mvn compile

# Ejecutar tests
mvn test

# Levantar la aplicación (perfil dev por defecto, puerto 8082)
mvn spring-boot:run

# Levantar con perfil qa (CSRF y cookies secure activos)
mvn spring-boot:run -Dspring-boot.run.profiles=qa

# Empaquetar JAR
mvn package -DskipTests
```

## Perfiles

| Perfil | Uso | CSRF | Cookie `secure` |
|--------|-----|------|-----------------|
| `dev` (por defecto) | Desarrollo local / Postman | Desactivado | No |
| `qa` | QA / producción | Activado | Sí |

En qa/producción exporta `SPRING_PROFILES_ACTIVE=qa`; si no se sobreescribe, la app arranca en modo
`dev` inseguro.

## Documentación adicional

- [CLAUDE.md](CLAUDE.md) — arquitectura hexagonal, convenciones de código y checklist para agentes AI
- [docs/jwt-spring-security-guide.md](docs/jwt-spring-security-guide.md) — implementación de JWT + Spring Security
- [docs/security-config.md](docs/security-config.md) — guía de `SecurityConfig` (filtros, CORS, CSRF, beans)
- [docs/push-reminders.md](docs/push-reminders.md) — recordatorios push de citas (Web Push): endpoints `/api/v1/push`, job, claves VAPID y seguridad
- [docs/ai-credits.md](docs/ai-credits.md) — créditos diarios de AI del asistente (`GET /api/v1/agent/credits`, `429` al agotarse): cobro por tokens reales, tope global y configuración
- [docs/voice-transcription.md](docs/voice-transcription.md) — dictado por voz del asistente (`POST /api/v1/agent/transcriptions`): cuotas, control de gasto y configuración
- [docs/jwt-secret.md](docs/jwt-secret.md) — secreto HS256 del JWT (`JWT_SECRET`): generación, carga, Parameter Store y rotación
- [docs/rate-limiting.md](docs/rate-limiting.md) — rate limiting por IP con Bucket4j + Caffeine
- [docs/actuator-health.md](docs/actuator-health.md) — `GET /actuator/health` (único endpoint de Actuator expuesto) para ECS Fargate/ALB
- [docs/flyway-migrations.md](docs/flyway-migrations.md) — cómo escribir migraciones
- [docs/agent-flows.md](docs/agent-flows.md) — flujos del agente de IA
- [docs/customer-multi-tenancy.md](docs/customer-multi-tenancy.md) — multi-tenancy por cliente
