# BookingApp — Backend

API REST para gestión de reservas y citas, construida con Spring Boot y arquitectura hexagonal.

## Stack

- **Java 25** / **Spring Boot 4.x**
- **Spring Security** + **JWT RS256** (OAuth2 Resource Server)
- **JPA / Hibernate** — PostgreSQL
- **Spring AI** — DeepSeek Flash (agente de reservas por lenguaje natural)
- **MapStruct** + **Lombok**
- **Maven**

## Prerrequisitos

- JDK 25+
- Maven 3.9+
- PostgreSQL 16+ corriendo localmente (o vía Docker)
- Par de claves RSA para firmar/verificar JWT (ver `docs/jwt-spring-security-guide.md`)

## Configuración

### 1. Base de datos

```sql
CREATE DATABASE bookingapp;
```

### 2. Variables de entorno

Crea un archivo `.env` en la raíz del módulo backend (nunca lo comitas):

```properties
DB_URL=jdbc:postgresql://localhost:5432/bookingapp
DB_USERNAME=postgres
DB_PASSWORD=tu_password

DEEPSEEK_API_KEY=tu_api_key_deepseek
```

### 3. Claves RSA

Genera el par de claves y copia las rutas en `application.yaml`:

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

En `src/main/resources/application.yaml`:

```yaml
security:
  jwt:
    private-key: classpath:certs/private.pem
    public-key: classpath:certs/public.pem
    expiration: 60        # minutos
```

Coloca los archivos `.pem` en `src/main/resources/certs/` (añade esa carpeta al `.gitignore`).

## Comandos

```bash
# Compilar
mvn compile

# Ejecutar tests
mvn test

# Levantar la aplicación
mvn spring-boot:run

# Empaquetar JAR
mvn package -DskipTests
```

## Endpoints principales

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | Público | Registrar usuario |
| POST | `/api/v1/auth/login` | Público | Obtener JWT |
| GET | `/api/v1/customers` | Bearer JWT | Listar clientes |
| POST | `/api/v1/appointments` | Bearer JWT | Crear cita |
| POST | `/api/v1/agent/chat` | Bearer JWT | Chat con agente AI |

## Arquitectura

Ver [CLAUDE.md](CLAUDE.md) para la guía completa de arquitectura hexagonal, convenciones de código y checklist para agentes AI.

Para la implementación de JWT + Spring Security ver [docs/jwt-spring-security-guide.md](docs/jwt-spring-security-guide.md).
