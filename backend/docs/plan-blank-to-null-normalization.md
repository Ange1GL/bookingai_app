# Plan: dónde vive `blankToNull` (ListCustomersService)

## Contexto
`ListCustomersService.blankToNull(String)` convierte un filtro en blanco en `null` (y hace `strip`). Pregunta: ¿la arquitectura hexagonal pide moverlo a una clase `Utils`?

## Qué dice la arquitectura hexagonal
Nada sobre clases utilitarias. La única regla es la dirección de dependencias: `domain` y `application` no dependen de `infrastructure` ni de detalles de framework. Por eso:
- Una `StringUtils` en `infrastructure/` usada desde `application/` **viola** la regla.
- Una utilidad pura Java en `domain/shared/` (junto a `Result`, `AuthError`) **no** la viola.
- Spring `StringUtils.hasText` en `application/` es aceptable en la práctica (los servicios ya usan `@Service`/`@Transactional`) pero no es puro.

## Situación en el código
| Lugar | Qué hace | ¿Mismo propósito? |
|-------|----------|-------------------|
| `ListCustomersService.blankToNull` | normaliza filtro opcional (`strip` → `null`) | — |
| `LogoutService` (x2), `PriceCatalog` | `== null \|\| isBlank()` para **rechazar** | No: validan, no normalizan |

Solo hay **un** uso de normalización. La regla DRY del proyecto dice esperar a la tercera repetición (Rule of Three).

## Opciones
1. **Dejarlo privado** (cero cambios). Válido mientras haya un solo uso.
2. **Mover la normalización a `ListCustomersQuery`** (recomendada). Constructor compacto del record que hace `name = blankToNull(name)` y `phone = blankToNull(phone)`. El invariante "un filtro nunca es blanco" queda garantizado por el propio objeto, sea cual sea el origen (REST, AI tool, tests), y `ListCustomersService.normalize` desaparece.
3. **Clase utilitaria en `domain/shared/` (p. ej. `Texts`)**, pura Java, solo cuando aparezca el 2.º/3.er uso real de normalización.

## Pasos propuestos (opción 2)
1. En `application/query/ListCustomersQuery`, añadir constructor compacto que normaliza `name` y `phone` (helper `private static String blankToNull`).
2. En `ListCustomersService`, eliminar `normalize` y `blankToNull`; pasar `query` directo a `findPage`.
3. Mover los asserts de normalización de `ListCustomersServiceTest` a un `ListCustomersQueryTest` (blank → `null`, `strip`), dejando en el servicio solo la delegación.
4. Ajustar `backend/docs/customer-list.md` si menciona dónde se normaliza.
5. `./mvnw test` y commit `refactor(customer): normalize list filters in query object`.

## Criterio para pasar a opción 3
Si otro caso de uso necesita la misma normalización (3.er uso), extraer a `domain/shared/Texts.blankToNull` (final, constructor privado, sin dependencias) y reutilizar desde ambos.
