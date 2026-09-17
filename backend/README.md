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
  **RS256** (clave privada) y los valida con la clave pública. Los tokens viajan en cookies `HttpOnly`.
- El agente de IA (DeepSeek vía Spring AI) usa tools (`@Tool` en `BookingTools`) para ejecutar los
  casos de uso de reservas; el LLM decide *qué* llamar, el backend decide *cómo*.
- El schema de PostgreSQL lo gobierna **Flyway** (Hibernate en `ddl-auto: validate`).

La guía completa de capas, convenciones y checklist está en [CLAUDE.md](CLAUDE.md).

## Stack

- **Java 25** / **Spring Boot 4.x**
- **Spring Security** + **JWT RS256** (OAuth2 Resource Server, tokens en cookies `HttpOnly`)
- **JPA / Hibernate** — PostgreSQL
- **Flyway** — migraciones versionadas del schema
- **Spring AI** — DeepSeek Flash (agente de reservas por lenguaje natural con tools)
- **MapStruct** + **Lombok**
- **Maven**

## Prerrequisitos

- JDK 25+
- Maven 3.9+ (o el wrapper `mvnw` incluido)
- PostgreSQL 16+ corriendo localmente (o vía Docker)
- OpenSSL (para generar el par de claves RSA — ver más abajo)
- API key de DeepSeek

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

# Opcional: rutas externas a las claves (por defecto se leen de classpath:certs/)
# JWT_PRIVATE_KEY_PATH=file:C:/keys/bookingapp/private.pem
# JWT_PUBLIC_KEY_PATH=file:C:/keys/bookingapp/public.pem
```

### 3. Claves RSA para JWT (cifrado asimétrico)

Los JWT se firman con **RS256**: la **clave privada** firma los tokens que emite el backend y la
**clave pública** los verifica. Spring carga las claves desde `application.yaml`:

```yaml
security:
  jwt:
    private-key: ${JWT_PRIVATE_KEY_PATH:classpath:certs/private.pem}
    public-key: ${JWT_PUBLIC_KEY_PATH:classpath:certs/public.pem}
    expiration: 15               # minutos, access token
    refresh-expiration: 43200    # minutos (30 días), refresh token
    issuer: bookingapp-api
    audience: bookingapp-frontend
```

> **Formato requerido:** Spring convierte la clave privada con `RsaKeyConverters.pkcs8()`, así que el
> archivo **debe ser PKCS#8** (cabecera `-----BEGIN PRIVATE KEY-----`). Una clave PKCS#1
> (`-----BEGIN RSA PRIVATE KEY-----`) hará fallar el arranque. La pública debe ser X.509
> (`-----BEGIN PUBLIC KEY-----`).

#### Paso 1 — Instalar OpenSSL en Windows

Elige **una** opción:

**Opción A — winget (recomendada):**

```powershell
winget install ShiningLight.OpenSSL.Light
```

Cierra y vuelve a abrir la terminal para que se refresque el `PATH`. Si `openssl` no se reconoce,
agrégalo manualmente (ruta por defecto del instalador):

```powershell
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\OpenSSL-Win64\bin", "User")
```

**Opción B — Chocolatey:**

```powershell
choco install openssl
```

**Opción C — Git for Windows (ya lo tienes si usas Git):** Git incluye un `openssl.exe`. Puedes
ejecutar los comandos desde **Git Bash**, o desde PowerShell invocándolo con su ruta completa:

```powershell
& "C:\Program Files\Git\usr\bin\openssl.exe" version
```

Verifica la instalación:

```powershell
openssl version
```

#### Paso 2 — Generar el par de claves

Desde la raíz del módulo `backend`, en PowerShell:

```powershell
New-Item -ItemType Directory -Force src\main\resources\certs
```

Generar la **clave privada** (2048 bits, sale directamente en formato PKCS#8):

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src\main\resources\certs\private.pem
```

Derivar la **clave pública** a partir de la privada:

```powershell
openssl rsa -in src\main\resources\certs\private.pem -pubout -out src\main\resources\certs\public.pem
```

#### Paso 3 — Verificar el formato

```powershell
Get-Content src\main\resources\certs\private.pem -TotalCount 1
Get-Content src\main\resources\certs\public.pem -TotalCount 1
```

Debe mostrar `-----BEGIN PRIVATE KEY-----` y `-----BEGIN PUBLIC KEY-----` respectivamente.

Si generaste la privada con `openssl genrsa` en una versión antigua de OpenSSL y la cabecera dice
`-----BEGIN RSA PRIVATE KEY-----` (PKCS#1), conviértela a PKCS#8:

```powershell
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_pkcs8.pem
```

y usa `private_pkcs8.pem` como `private.pem`.

#### Seguridad de las claves

- **Nunca comitees `private.pem`.** Asegúrate de que `src/main/resources/certs/` esté en `.gitignore`.
  Si una clave privada llegó al repositorio, considérala comprometida y genera un par nuevo.
- En qa/producción no empaquetes las claves en el JAR: móntalas en disco y apunta a ellas con
  `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` (prefijo `file:`).
- La clave pública sí puede compartirse con cualquier servicio que necesite validar los tokens.

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
- [docs/flyway-migrations.md](docs/flyway-migrations.md) — cómo escribir migraciones
- [docs/agent-flows.md](docs/agent-flows.md) — flujos del agente de IA
- [docs/customer-multi-tenancy.md](docs/customer-multi-tenancy.md) — multi-tenancy por cliente
