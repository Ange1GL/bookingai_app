# Bloqueo temporal de cuenta por intentos fallidos

Lógica de dominio en [`AccountBlocked`](../src/main/java/com/github/angellariosacosta/bookingapp/domain/model/AccountBlocked.java), consumida desde [`AuthenticateUserService`](../src/main/java/com/github/angellariosacosta/bookingapp/application/service/AuthenticateUserService.java) y limpiada por el job [`ResetAccountLockedJob`](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/adapter/in/scheduler/ResetAccountLockedJob.java).

---

## Cómo funciona

- Cada usuario tiene, como mucho, una fila en `account_blocked` (`userId` es la clave de negocio; ver `AccountBlockedRepository.findByUserId`).
- `NUMBER_OF_ATTEMPTS_ALLOWED = 3`: al 3er login fallido consecutivo la cuenta queda bloqueada.
- `BLOCKING_TIME = 30` minutos: ventana de bloqueo, calculada en `addBlockingTime()` (`startExpiratedAt` = ahora, `endExpiratedAt` = ahora + 30 min).
- En cada intento de login, `AuthenticateUserService.authenticate()` llama primero a `AccountBlocked.ensureNotBlocked()` (si existe fila para ese usuario). Si `numberOfAttempts >= 3`, lanza `AccountBlockedException` **antes** de validar la contraseña.
- Si la contraseña es incorrecta y la cuenta no estaba bloqueada, se incrementa el contador (`incrementNumberOfAttempts()`) y se persiste.
- Si el login es exitoso, se resetea el contador y la ventana de bloqueo (`AccountBlockedRepository.resetAttempts`) — esto es lo que hace que 2 fallos + 1 acierto vuelvan la cuenta a estado limpio, sin necesidad de esperar nada.
- El desbloqueo automático **por tiempo** (cuando sí se llega al umbral) no ocurre de forma perezosa en el login: lo hace `ResetAccountLockedJob`, que corre cada `security.account-lock.reset-cleanup.interval-minutes` (5 min por defecto, `application.yaml`) y resetea toda fila cuyo `endExpiratedAt` ya haya pasado. Es un diseño intencional (ver comentario en el job): no hay un único temporizador global, cada cuenta expira según su propio `endExpiratedAt`.

## Bug corregido: el bloqueo se volvía permanente

`incrementNumberOfAttempts()` tenía un off-by-one:

```java
// Antes
public void incrementNumberOfAttempts() {
    if (numberOfAttempts <= NUMBER_OF_ATTEMPTS_ALLOWED) {
        this.numberOfAttempts++;
        return;
    }
    addBlockingTime(); // solo se ejecutaba si numberOfAttempts ya era > 3
}
```

Como `isBlocked()` corta el login (con `numberOfAttempts >= 3`) **antes** de que `manageAccountLocked()` pueda volver a llamar `incrementNumberOfAttempts()`, el estado `numberOfAttempts > 3` nunca se alcanzaba. Consecuencia: `addBlockingTime()` nunca se ejecutaba, `endExpiratedAt` se quedaba `null` para siempre, y la query de `ResetAccountLockedJob` (`WHERE endExpiratedAt IS NOT NULL AND endExpiratedAt <= cutoff`) jamás encontraba la fila. La cuenta quedaba bloqueada indefinidamente, incluso con la contraseña correcta.

Fix: disparar `addBlockingTime()` en el mismo paso en que se cruza el umbral (3er intento), no uno después:

```java
public void incrementNumberOfAttempts() {
    if (numberOfAttempts < NUMBER_OF_ATTEMPTS_ALLOWED) {
        this.numberOfAttempts++;
        if (numberOfAttempts == NUMBER_OF_ATTEMPTS_ALLOWED) {
            addBlockingTime();
        }
    }
}
```

Con esto `endExpiratedAt` queda poblado en cuanto la cuenta se bloquea, y `ResetAccountLockedJob` puede encontrarla y liberarla pasados los 30 minutos (más el intervalo de polling del job). El reset-on-success de `AuthenticateUserService` (línea con `accountBlockedRepository::resetAttempts`) ya era correcto y no se modificó — el problema nunca fue la ausencia de ese reset, sino que el camino para llegar a él tras un bloqueo real (login correcto una vez expirada la ventana) dependía de un campo que nunca se llenaba.

Cobertura: [`AccountBlockedTest`](../src/test/java/com/github/angellariosacosta/bookingapp/domain/model/AccountBlockedTest.java) fija el comportamiento del umbral, la ventana de bloqueo y el reset.

## Ronda 2: encapsulamiento, naming y condición de carrera

- **Encapsulamiento**: los campos de `AccountBlocked` eran package-private (sin modificador), mutables desde cualquier clase de `domain.model`. Ahora son `private`. El constructor `AccountBlocked(Long userId)` duplicaba al factory `create(Long userId)`; se dejó `create()`/`reconstitute()` como únicos puntos de entrada públicos y el constructor pasó a `private`.
- **Naming**: `isBlocked()` sugería un predicado `boolean` pero lanzaba una excepción. Se renombró a `ensureNotBlocked()`.
- **Condición de carrera en el contador**: `manageAccountLocked()` (ahora `registerFailedAttempt()`) leía `AccountBlocked` sin lock, incrementaba en memoria Java y persistía el valor absoluto ya calculado. Dos intentos fallidos concurrentes del mismo usuario podían pisarse (lost update): ambos leen `numberOfAttempts = N`, ambos calculan `N + 1` y el segundo `UPDATE` sobrescribe con el mismo valor que el primero, en vez de `N + 2` — el contador "pierde" un fallo real.

  Fix: `AuthenticateUserService.authenticate()` ahora es `@Transactional`, y `registerFailedAttempt()` lee la fila con `AccountBlockedRepository.findByUserIdForUpdate()` — un `SELECT ... FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` en `JpaRepositoryAccountBlocked`) que toma un lock exclusivo de fila hasta que la transacción termina. Un segundo intento fallido concurrente para el mismo usuario queda esperando ese lock, y cuando lo obtiene ya ve el valor actualizado por el primero — sin lost update. Se prefirió el lock sobre un `UPDATE ... SET numberOfAttempts = numberOfAttempts + 1` directo en SQL porque este último obligaría a duplicar el umbral `3` y la decisión de cuándo fijar la ventana de bloqueo fuera del dominio (`AccountBlocked.incrementNumberOfAttempts()`/`addBlockingTime()`), violando la separación de capas del proyecto.
- **Migración `V6__add_unique_constraint_account_blocked_user_id.sql`**: se encontró que `account_blocked.user_id` no tenía constraint `UNIQUE` (`V4` solo define la FK). Sin ella, dos primeros-intentos-fallidos concurrentes para un usuario sin fila previa podían insertar dos filas duplicadas vía la rama `AccountBlocked.create()`, rompiendo después `findByUserId` (`Optional` esperando una sola fila).
- **Orden de validación**: `authenticate()` ahora valida `user.isActive()` antes que la contraseña, para no gastar el costo de BCrypt (ni exponer una diferencia de tiempo de respuesta) en cuentas ya deshabilitadas.
