# Multi-tenancy en `Customer`: asociación con `userId`

`Customer` ahora está asociado al `User` (barbero) que lo registró, para soportar un modelo SaaS donde varios barberos usan la misma app sin ver los clientes de los demás.

---

## 1. Problema que resuelve

Antes de este cambio, `Customer` no tenía ningún vínculo con `User`: todas las consultas (`findById`, `findByPhone`, búsqueda por nombre) eran globales. En un modelo de un solo barbero eso no importaba, pero al pasar a SaaS con varios barberos registrando clientes en la misma base de datos, esto generaba colisiones:

- Un barbero podía, en teoría, encontrar o referenciar (por ID) clientes registrados por otro barbero.
- Dos barberos con un cliente del mismo teléfono terminarían compartiendo el mismo registro de `Customer`.

## 2. Decisión de diseño: ID escalar, no objeto `User` completo

`Customer` se relaciona con `User` mediante `private Long userId;` — **no** mediante un campo `private User user;`. Esto sigue el mismo patrón que ya usaba `Appointment` para su relación con el barbero dueño (`Appointment.userId`), a diferencia de su relación con `Customer` (`Appointment.customer`), que sí embebe el objeto completo porque el caso de uso necesita operar directamente con los datos del cliente.

La regla general: cuando la relación entre dos agregados es de **pertenencia/tenancy** (¿de quién es este registro?) y no de **colaboración de negocio** (necesito los datos del otro agregado para decidir algo), basta con el ID. Ningún servicio de `Customer` necesita cargar roles, permisos o el password de `User` — solo necesita saber a quién pertenece el registro para filtrar.

A nivel de persistencia (`CustomerEntity`), se replica el patrón de doble campo ya documentado en `CLAUDE.md` y usado en `AppointmentEntity`: una columna escalar de escritura (`userId`) más un campo `@ManyToOne` de solo lectura (`user`) para navegación/joins:

```java
@Column(name = "user_id")
private Long userId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", referencedColumnName = "user_id", updatable = false, insertable = false)
private UserEntity user;
```

No se requirió script de migración: `application.yaml` tiene `hibernate.ddl-auto: update`, así que Hibernate agregó la columna sola al levantar la app.

## 3. Cómo se propaga el `userId`

El `userId` nunca viaja en el body de una request REST ni se pide explícitamente al modelo de IA — se resuelve siempre del usuario autenticado actual:

- En REST: vía `@CurrentUserId Long userId` (mismo `HandlerMethodArgumentResolver` que ya usa `AppointmentController`).
- En las tools de IA (`BookingTools`): vía `CurrentUserPort.getCurrentUserId()`, inyectado directamente.

Desde ahí baja como campo `userId` en los *commands* de aplicación (`CreateCustomerCommand`, `CreateAppointmentCommand`, `BookAppointmentCommand`, etc.) y se usa para filtrar en el puerto `CustomerRepository`:

```java
Optional<Customer> findById(Long id, Long userId);
Optional<Customer> findByPhone(String phone, Long userId);
List<Customer> searchByNameContaining(String name, Long userId);
```

## 4. Reglas de comportamiento

- **`findById` cruzado entre usuarios se trata como "no encontrado".** Si el `customerId` existe pero pertenece a otro usuario, `CustomerRepositoryJpa.findByIdAndUserId(id, userId)` no lo encuentra, y el servicio lanza el mismo `CustomerNotFoundException` que usaría si el ID no existiera en absoluto. No se distingue "forbidden" de "not found" para no filtrar información sobre la existencia de recursos ajenos.
- **`CreateCustomerService` hace find-or-create por `phone` + `userId`.** Si el barbero autenticado ya tiene un cliente con ese teléfono, se reutiliza en vez de crear un duplicado. Esto es scoped por usuario: dos barberos pueden tener, cada uno, un cliente con el mismo número de teléfono, y quedan como dos registros de `Customer` independientes.
- **`BookAppointmentService`** sigue el mismo find-or-create por teléfono, ahora scoped por `userId` (antes de este cambio, el `Customer` creado en este flujo ni siquiera quedaba asociado a ningún usuario).
- **`SearchCustomersUseCase`** quedó actualizado para aceptar `userId`, pero de momento solo lo consume la tool de IA (`BookingTools.searchCustomersByName`) — no hay un endpoint REST de búsqueda todavía.
