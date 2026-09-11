# Especificación del Proyecto — Guía para Agentes de IA

Este documento define las reglas, convenciones y estructura que **todo agente de IA** debe seguir al generar, modificar o revisar código en este proyecto. El objetivo es mantener consistencia arquitectónica y de estilo en todo el codebase.

---

## Stack Tecnológico

- **Lenguaje:** Java
- **Framework:** Spring Boot
- **Persistencia:** JPA / Hibernate
- **Base de datos:** Relacional (PostgreSQL, MySQL, SQL Server, etc. — el motor específico puede variar según el entorno, pero el enfoque de mapeo siempre es el mismo)
- **Build tool:** Maven o Gradle (según lo que ya esté configurado en el repositorio)

> **Nota:** Este documento no fija versiones específicas de Java, Spring Boot ni la herramienta de build. El agente debe **inspeccionar el `pom.xml` / `build.gradle` existente** para determinar las versiones reales antes de generar código, y mantener compatibilidad con ellas.

---

## Arquitectura: Hexagonal (Ports & Adapters)

El proyecto sigue **arquitectura hexagonal**. Todo el código debe organizarse respetando la separación entre **dominio**, **aplicación** e **infraestructura**, evitando que el dominio dependa de frameworks externos.

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
        insertable = false
)
private CustomerEntity customer;
```

### Reglas de este patrón

- El campo **escalar** (`customerId`) es el que se usa para **escribir/actualizar** la relación (insert/update). Es el dueño real de la FK.
- El campo de **relación de objeto** (`customer`) es **solo de lectura** (`insertable = false, updatable = false`) y se usa exclusivamente para **navegación/lectura** (evitar N+1 innecesarios, fetch explícito, etc.).
- `referencedColumnName` debe apuntar siempre al nombre de columna real de la PK/columna referenciada en la tabla destino (no asumir siempre `"id"`; debe coincidir con el `@Column` correspondiente en la entidad referenciada).
- Este patrón aplica también a relaciones `@ManyToOne` y, cuando corresponda, al lado inverso de `@OneToMany` (usando `mappedBy`).
- **Nunca** usar únicamente `@ManyToOne`/`@OneToOne` sin el campo escalar acompañante: siempre debe generarse el par (campo FK + campo de relación).
- Al generar nuevas entidades con relaciones, el agente **debe replicar este mismo patrón exacto**, ajustando nombres de columna, tipo de relación (`@OneToOne`, `@ManyToOne`, `@OneToMany`) y `referencedColumnName` según corresponda.

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
- [ ] ¿La entidad JPA está en `infrastructure.adapter.out.persistence.entity` y no se filtra al dominio?
- [ ] ¿Toda relación entre entidades sigue el patrón de campo escalar (FK) + campo de relación de solo lectura?
- [ ] ¿Los puertos (`port.in` / `port.out`) están definidos como interfaces en `domain`?
- [ ] ¿La lógica de negocio vive en `application.service`, no en el controlador ni en el adaptador de persistencia?
- [ ] ¿Existe un mapper entre `Entity` y modelo de `domain`, y entre `dto` y modelo de `domain`?
- [ ] ¿Revisé las versiones reales de Java/Spring Boot/build tool en el proyecto antes de usar sintaxis o dependencias específicas?

---


## Detalles del código

- Prioiza escribir ingles, solamente sera en español los mensajes o comentarios de algun linea en espefico
- Evita usar a lo maximo var, es tu ultima opción usar la declarion var de Java
- 

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