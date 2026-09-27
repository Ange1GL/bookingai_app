# Especificación del Proyecto — Guía para Agentes de IA

Este documento define las reglas, convenciones y estructura que **todo agente de IA** debe seguir al generar, modificar o revisar código en este proyecto. El objetivo es mantener consistencia arquitectónica y de estilo en todo el codebase.

---

## Stack Tecnológico

- **Lenguaje:** Java
- **Framework:** Spring Boot
- **Persistencia:** JPA / Hibernate
- **Base de datos:** Relacional (PostgreSQL, MySQL, SQL Server, etc. — el motor específico puede variar según el entorno, pero el enfoque de mapeo siempre es el mismo)
- **Migraciones de schema:** Flyway (`src/main/resources/db/migration/`). Hibernate está en `ddl-auto: validate` y **no** modifica el schema: toda creación/alteración de tabla o columna va en una migración versionada. Ver `docs/flyway-migrations.md`.
- **Build tool:** Maven o Gradle (según lo que ya esté configurado en el repositorio)

> **Nota:** Este documento no fija versiones específicas de Java, Spring Boot ni la herramienta de build. El agente debe **inspeccionar el `pom.xml` / `build.gradle` existente** para determinar las versiones reales antes de generar código, y mantener compatibilidad con ellas.

---

## Arquitectura: Hexagonal (Ports & Adapters)

El proyecto sigue **arquitectura hexagonal**. Todo el código debe organizarse respetando la separación entre **dominio**, **aplicación** e **infraestructura**, evitando que el dominio dependa de frameworks externos.

> **Excepción deliberada — `@Service` en la capa application:**  
> Aunque la arquitectura hexagonal pura evita anotaciones de framework fuera de infraestructura, en este proyecto **los servicios de aplicación (`application/service/`) SÍ usan `@Service`** para que Spring los registre como beans y los gestione en el contexto de la aplicación. El dominio (`domain/`) sigue siendo Java puro sin ninguna anotación de Spring o JPA.

### Capas

```
src/main/java/com/empresa/proyecto/
├── domain/
│   ├── model/              # Entidades de dominio (POJOs puros, sin anotaciones de JPA/Spring)
│   ├── exception/           # Excepciones propias del dominio
│   └── port/
│       ├── in/               # Puertos de entrada (casos de uso, interfaces)
│       └── out/              # Puertos de salida (repositorios, gateways, interfaces)
│
├── application/
│   ├── command/              # Entradas a casos de uso (ej. BookAppointmentCommand)
│   ├── result/               # Salidas de casos de uso (ej. AuthTokenResult)
│   └── service/              # Implementación de los casos de uso (implementan port.in)
│                              # Orquestan lógica de negocio usando los port.out
│
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/          # Controladores REST (adaptadores de entrada)
    │   │       ├── controller/
    │   │       ├── dto/
    │   │       └── mapper/
    │   └── out/
    │       └── persistence/   # Adaptadores de salida hacia la base de datos
    │           ├── entity/     # Entidades JPA (@Entity)
    │           ├── repository/ # Interfaces Spring Data JPA
    │           ├── mapper/     # Mapeo Entity <-> Domain Model
    │           └── adapter/    # Implementan los port.out usando el repository
    └── config/                # Configuración de Spring (beans, seguridad, etc.)
```

### Reglas de dependencia

1. **`domain`** no depende de ninguna otra capa ni de Spring/JPA. Es Java puro.
2. **`application`** depende solo de `domain` (implementa los puertos de entrada y consume los puertos de salida mediante interfaces).
3. **`infrastructure`** depende de `domain` y `application`, nunca al revés.
4. La comunicación entre capas siempre se hace a través de **interfaces (puertos)**, nunca accediendo directamente a implementaciones concretas.
5. Las entidades JPA (`@Entity`) **viven únicamente en `infrastructure.adapter.out.persistence.entity`** y nunca se exponen fuera de esa capa. Se mapean a modelos de dominio mediante un `mapper`.
6. Los DTOs de entrada/salida REST viven en `infrastructure.adapter.in.rest.dto` y se mapean al modelo de dominio, nunca se exponen las entidades de dominio directamente en la API (salvo que el propio equipo decida lo contrario explícitamente).

---

## Convención de Relaciones JPA

Para **todas las relaciones entre entidades** (OneToOne, ManyToOne, OneToMany según aplique) se debe usar el patrón de **doble campo**: un campo escalar con el FK (`insertable`/`updatable` habilitado) y un campo de relación de solo lectura (`insertable = false`, `updatable = false`) para navegación.

### Patrón obligatorio

```java
@Column(name = "customer_id")
private Long customerId;

@OneToOne
@JoinColumn(
        name = "customer_id",
        referencedColumnName = "customer_id",
        updatable = false,
        insertable = false,
        foreignKey = @ForeignKey(name = "fk_appointment_customer")
)
private CustomerEntity customer;
```

### Reglas de este patrón

- El campo **escalar** (`customerId`) es el que se usa para **escribir/actualizar** la relación (insert/update). Es el dueño real de la FK.
- El campo de **relación de objeto** (`customer`) es **solo de lectura** (`insertable = false, updatable = false`) y se usa exclusivamente para **navegación/lectura** (evitar N+1 innecesarios, fetch explícito, etc.).
- `referencedColumnName` debe apuntar siempre al nombre de columna real de la PK/columna referenciada en la tabla destino (no asumir siempre `"id"`; debe coincidir con el `@Column` correspondiente en la entidad referenciada).
- **Toda FK lleva nombre explícito** con `foreignKey = @ForeignKey(name = "fk_<tabla_origen>_<campo>")`. Sin él, Hibernate genera un hash ilegible (`FKmyowslj1th8d9j6j3wlbwrtoe`) que termina en las migraciones Flyway y en los mensajes de error de la BD.
- Este patrón aplica también a relaciones `@ManyToOne` y, cuando corresponda, al lado inverso de `@OneToMany` (usando `mappedBy`).
- **Nunca** usar únicamente `@ManyToOne`/`@OneToOne` sin el campo escalar acompañante: siempre debe generarse el par (campo FK + campo de relación).
- Al generar nuevas entidades con relaciones, el agente **debe replicar este mismo patrón exacto**, ajustando nombres de columna, tipo de relación (`@OneToOne`, `@ManyToOne`, `@OneToMany`), `referencedColumnName` y el nombre de la FK según corresponda.

### Constraints UNIQUE: siempre con nombre, a nivel de `@Table`

`@Column(unique = true)` no admite nombre y genera `UK_<hash>`. Las unique se declaran en `@Table` con `@UniqueConstraint` nombrado, y en la columna **no** se pone `unique = true` (si se deja, Hibernate genera dos constraints):

```java
@Table(
        name = "user_app",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_app_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_app_email", columnNames = "email")
        }
)
public class UserEntity extends BaseEntity {

    @Column(nullable = false)   // sin unique = true
    private String username;
}
```

Convención de nombres: `fk_<tabla_origen>_<campo>` para foreign keys, `uk_<tabla>_<columna>` para unique. Minúsculas y guiones bajos, igual que tablas y columnas.

---

## Convenciones Generales de Código

- **Nombres de clases de entidad JPA:** sufijo `Entity` (ej. `CustomerEntity`, `OrderEntity`).
- **Nombres de modelos de dominio:** sin sufijo (ej. `Customer`, `Order`).
- **Repositorios Spring Data:** interfaz `XxxJpaRepository extends JpaRepository<XxxEntity, ID>` dentro de `infrastructure.adapter.out.persistence.repository`.
- **Adaptadores de salida:** clase `XxxRepositoryAdapter` que implementa el puerto `XxxRepositoryPort` (definido en `domain.port.out`), usando internamente el `JpaRepository` y el `mapper` correspondiente.
- **Casos de uso / servicios de aplicación:** implementan una interfaz de `domain.port.in` (ej. `CreateCustomerUseCase`) y se ubican en `application.service`.
- **Controladores REST:** delgados; solo reciben el request, validan/mapea a DTO, invocan el caso de uso (puerto de entrada) y devuelven la respuesta mapeada.
- **Mappers:** preferir MapStruct si ya está configurado en el proyecto; si no, mappers manuales explícitos. Nunca lógica de negocio dentro de un mapper.
- **Transacciones:** se gestionan en la capa `application` (`@Transactional` en los servicios de caso de uso), no en los adaptadores de infraestructura.
- **Excepciones de dominio:** se definen en `domain.exception` y se traducen a respuestas HTTP en un `@ControllerAdvice` dentro de `infrastructure.adapter.in.rest`.

---

##  Checklist para el Agente antes de generar código

- [ ] ¿Estoy respetando la separación domain / application / infrastructure?
- [ ] ¿Todo cambio en una entidad JPA (campo, tabla, constraint) viene acompañado de su migración Flyway en el mismo commit? Sin ella, `ddl-auto: validate` impide que la app arranque.
- [ ] ¿Si agregué un valor a un enum que refleja una tabla de catálogo (ej. `StatusAppointment` ↔ `status_appointment`), agregué una migración con su `INSERT`? El mapeo es por id (`fromId`); un valor en el enum sin fila en la BD viola la FK al usarse.
- [ ] ¿La entidad JPA está en `infrastructure.adapter.out.persistence.entity` y no se filtra al dominio?
- [ ] ¿Toda relación entre entidades sigue el patrón de campo escalar (FK) + campo de relación de solo lectura?
- [ ] ¿Los puertos (`port.in` / `port.out`) están definidos como interfaces en `domain`?
- [ ] ¿La lógica de negocio vive en `application.service`, no en el controlador ni en el adaptador de persistencia?
- [ ] ¿Existe un mapper entre `Entity` y modelo de `domain`, y entre `dto` y modelo de `domain`?
- [ ] ¿Revisé las versiones reales de Java/Spring Boot/build tool en el proyecto antes de usar sintaxis o dependencias específicas?
- [ ] ¿Toda dependencia se inyecta por constructor (`@RequiredArgsConstructor` o constructor explícito) y los campos son `final`?
- [ ] ¿No hay ningún `@Autowired` en campos de clases de producción?

---


## Detalles del código

- Prioriza escribir en inglés; solo serán en español los mensajes visibles al usuario o comentarios de línea específicos.
- Evita usar `var` al máximo — es tu **última opción**. Siempre declara el tipo explícito.

### Valores por defecto ante `null`: usar `Objects.requireNonNullElse`

**Regla obligatoria:** Cuando la lógica de negocio lo permita, para resolver un valor por defecto ante un posible `null` siempre usa `Objects.requireNonNullElse(valor, porDefecto)` en lugar de un `if`/ternario que compare explícitamente contra `null` (`valor == null ? ... : ...`, `valor != null ? ... : ...`).

Motivo: SonarLint (el linter del IDE) frecuentemente marca esas comparaciones manuales como *"the condition is always true/false"* en falsos positivos difíciles de silenciar. `Objects.requireNonNullElse` expresa la misma intención sin disparar ese warning.

#### Prohibido

```java
// MAL — dispara "condition is always true" en SonarLint
String detail = authException.getMessage() != null
        ? authException.getMessage()
        : "sin detalle";
```

#### Correcto

```java
// BIEN
String detail = Objects.requireNonNullElse(authException.getMessage(), "sin detalle");
```

Esto aplica solo cuando la semántica es "usar un valor por defecto si el original es `null`". No aplica si la rama `null` requiere lanzar una excepción, ejecutar lógica adicional, o si el valor por defecto es costoso de calcular (en ese caso usar `Objects.requireNonNullElseGet`).

### Tipos de retorno en REST: usar `record`, no `Map` ni wildcards

**Regla obligatoria:** Los controladores REST y el `@RestControllerAdvice` **nunca** deben devolver `Map<String, Object>` ni usar `?` (wildcard) en `ResponseEntity<?>`. Siempre usa un `record` con nombre y tipo explícito.

#### Prohibido

```java
// MAL — wildcard sin tipo concreto
public ResponseEntity<?> login(@RequestBody LoginRequest request) { ... }

// MAL — Map como cuerpo de respuesta
private ResponseEntity<Map<String, Object>> toErrorResponse(AuthError error) { ... }

// MAL — Map en GlobalExceptionHandler
public Map<String, Object> handleNotFound(CustomerNotFoundException ex) { ... }
```

#### Correcto

```java
// BIEN — record tipado, ubicado en infrastructure.adapter.in.rest.dto
public record ErrorResponse(String timestamp, int status, String error, String message) {}

// BIEN — tipo explícito en el controlador
public ResponseEntity<AuthSuccessResponse> login(@RequestBody LoginRequest request) { ... }

// BIEN — tipo explícito en el handler de errores
public ResponseEntity<ErrorResponse> toErrorResponse(AuthError error) { ... }

// BIEN — record en GlobalExceptionHandler
public ErrorResponse handleNotFound(CustomerNotFoundException ex) { ... }
```

Los records de respuesta se ubican en `infrastructure.adapter.in.rest.dto` y se construyen en un `XxxRestMapper` (`infrastructure.adapter.in.rest.mapper`), nunca inline en el controller. Un `record` de `application` (command/result) solo puede devolverse directo si contiene **exactamente** lo que la API debe exponer; si lleva datos sensibles el DTO es obligatorio. Ejemplo: `AuthTokenResult` incluye `accessToken`/`refreshToken`, que viajan solo en cookies `HttpOnly`, por eso `AuthRestMapper` lo proyecta a `AuthSuccessResponse` sin los tokens.

#### Caso especial: controlador con `Result<S, E>` y dos tipos de cuerpo

Cuando un controller usa el patrón `Result<S, E>` y los cuerpos de éxito y fallo son tipos distintos (ej. `AuthSuccessResponse` vs `ErrorResponse`), usar una **`sealed interface`** como tipo común. Nunca resolver con `?` ni con `Map`.

```java
// dto/AuthResponse.java
public sealed interface AuthResponse permits AuthSuccessResponse, ErrorResponse {}

// El controller retorna ResponseEntity<AuthResponse> — tipo explícito, sin wildcard
public ResponseEntity<AuthResponse> login(...) {
    if (result.isFailure()) {
        return toErrorResponse(result.getError()); // → ErrorResponse
    }
    return ResponseEntity.ok(new AuthSuccessResponse(...)); // → AuthSuccessResponse
}
```

### Checklist de tipos de retorno REST

- [ ] ¿El tipo de retorno del controller/handler es un `record` tipado en lugar de `Map<String, Object>`?
- [ ] ¿No hay ningún `ResponseEntity<?>` (wildcard) en controladores ni `@RestControllerAdvice`?
- [ ] ¿Cuando el body varía entre éxito y fallo, se usa una `sealed interface` como tipo común?
- [ ] ¿`var` se usa solo cuando no existe una alternativa más clara con tipo explícito?

---

## Inyección de Dependencias

**Regla obligatoria:** Usar siempre **inyección por constructor**. Prohibido usar `@Autowired` en campos o métodos, salvo las excepciones indicadas abajo.

### Forma correcta — Lombok `@RequiredArgsConstructor` (preferido)

```java
@Service
@RequiredArgsConstructor
public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final CustomerRepositoryPort customerRepository;
}
```

### Forma correcta — constructor explícito (cuando Lombok no está disponible o se prefiere claridad)

```java
@Service
public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final CustomerRepositoryPort customerRepository;

    public CreateBookingService(BookingRepositoryPort bookingRepository,
                                CustomerRepositoryPort customerRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
    }
}
```

### Excepciones permitidas para `@Autowired`

- Tests unitarios con `@SpringBootTest` o `@DataJpaTest` donde se inyecte el contexto completo.
- Clases de configuración (`@Configuration`) cuando Spring requiera inyección de método (`@Bean` con parámetros ya resueltos por el contenedor).

### Prohibido en todo el código de producción

```java
// MAL — nunca hacer esto en clases de producción
@Autowired
private BookingRepositoryPort bookingRepository;
```

### Checklist adicional

- [ ] ¿Todos los campos inyectados son `final`?
- [ ] ¿Se usa `@RequiredArgsConstructor` de Lombok o un constructor explícito?
- [ ] ¿No hay ningún `@Autowired` en campos de clases de producción?

## Configuración AI (DeepSeek)

El módulo de IA usa `spring-ai-starter-model-deepseek` (Spring AI 2.0.0-M6). Variable requerida en `.env`:

```properties
DEEPSEEK_API_KEY=<tu-api-key>
```

Modelo activo: `deepseek-flash` (= DeepSeek-V4.1-Flash, soporta function calling / tools y thinking mode). Base URL: `https://api.deepseek.com`. Configurado en `application.yaml` bajo `spring.ai.deepseek.chat.options`.

**Historial de proveedores:**
- Azure OpenAI → descartado: filtraba "11 de septiembre" (falso positivo en content safety). Ver `docs/azure-openai-content-filter-september11.md`.
- Mistral AI → descartado: tier Studio con rate limits muy bajos (HTTP 429 frecuentes).
- DeepSeek → actual: sin filtros geopolíticos, tier de pago con saldo prepago, modelo `deepseek-flash` (V4.1-Flash).

El patrón de tools usa `@Tool` sobre métodos en `BookingTools` y se registra vía `ChatClient.defaultTools()` en `AiConfig` — patrón correcto para Spring AI 2.x y provider-agnostic.

---

## Notas Finales

Este documento es la referencia de arquitectura y convenciones para cualquier tarea de generación o modificación de código en este repositorio. Ante cualquier ambigüedad no cubierta aquí, el agente debe **priorizar la consistencia con el código ya existente** en el proyecto por encima de preferencias generales.