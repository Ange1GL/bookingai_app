# Migraciones de base de datos con Flyway

## Por qué se dejó de usar `ddl-auto: update`

Antes el schema lo gestionaba Hibernate comparando las `@Entity` contra la BD y parcheando
diferencias. Eso tenía tres límites que se sintieron en el proyecto:

1. **No migra datos.** Al agregar `username` como `NOT NULL UNIQUE` en `user_app` hubo que vaciar
   la tabla a mano: Hibernate no puede rellenar valores para las filas existentes.
2. **No borra ni renombra.** `update` solo agrega columnas. Una columna renombrada deja la vieja
   en la tabla para siempre, sin que nadie se entere.
3. **No hay historial ni reproducibilidad.** No quedaba registro de qué cambió ni cuándo, y no se
   podía levantar una BD idéntica en otra máquina desde cero.

Ahora la fuente de verdad del schema es el SQL versionado en `src/main/resources/db/migration/`, y
Hibernate quedó en `ddl-auto: validate`: solo verifica al arrancar que las entidades coincidan con
las tablas reales. Si falta una migración, **la app no levanta** — esa es la red de seguridad.

## Cómo funciona

- Flyway corre automáticamente al arrancar la app, **antes** de que Hibernate inicialice el
  `EntityManagerFactory`. Ese orden es lo que permite que `validate` funcione.
- Crea y mantiene la tabla `flyway_schema_history` en la BD, donde registra cada migración
  aplicada con su checksum.
- En cada arranque compara el historial contra los archivos del classpath y aplica solo lo nuevo,
  en orden de versión.

Las dependencias son **tres** (`pom.xml`), sin `<version>` porque las gestiona
`spring-boot-starter-parent`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Las tres hacen falta, y las dos últimas son las que suelen olvidarse:

- **`spring-boot-flyway`** — la autoconfiguración. En Spring Boot 4 las autoconfiguraciones se
  separaron en un módulo por tecnología (`spring-boot-hibernate`, `spring-boot-jdbc`,
  `spring-boot-flyway`…); antes vivían todas en `spring-boot-autoconfigure`. **Sin este módulo,
  `flyway-core` queda en el classpath pero Flyway nunca se ejecuta**, y lo hace *en silencio*: no
  hay error ni advertencia, simplemente no aparece ninguna línea de Flyway en el log y la BD se
  queda vacía. Casi todos los tutoriales están escritos para Boot 3 y no lo mencionan.
- **`flyway-database-postgresql`** — desde Flyway 10 el soporte de cada motor vive en un módulo
  aparte; sin él Flyway arranca pero no reconoce PostgreSQL.

> **Cómo diagnosticarlo:** si sospechas que Flyway no está corriendo, busca sus líneas en el log de
> arranque (`grep -i flyway`). Si no hay **ninguna**, falta `spring-boot-flyway`. El síntoma típico
> es que Hibernate falle después con `Schema validation: missing table [...]`, porque encontró la
> base vacía.

## Convención de nombres

```
V<versión>__<descripción>.sql
```

**Dos guiones bajos** entre la versión y la descripción. Ejemplos:

```
V1__baseline_schema.sql
V2__seed_status_appointment.sql
V3__add_phone_to_customer.sql
```

## Flujo de trabajo: agregar un cambio

Todo cambio de BD son dos cosas en el mismo commit: **la entidad JPA y su migración**.

1. Modifica la `@Entity` en `infrastructure.adapter.out.persistence.entity`.
2. Crea `V<siguiente>__<descripcion>.sql` en `src/main/resources/db/migration/`.
3. Levanta la app. Flyway aplica la migración; Hibernate valida que entidad y tabla coincidan.

Ejemplo real — así se habría hecho el cambio de `username` que se resolvió vaciando la tabla:

```sql
-- V3__add_username_to_user_app.sql
ALTER TABLE user_app ADD COLUMN username VARCHAR(255);
UPDATE user_app SET username = split_part(email, '@', 1) WHERE username IS NULL;  -- backfill
ALTER TABLE user_app ALTER COLUMN username SET NOT NULL;
ALTER TABLE user_app ADD CONSTRAINT uk_user_app_username UNIQUE (username);
```

Ese `UPDATE` intermedio es justo lo que Hibernate no puede hacer, y corre igual en tu máquina, en
la de un compañero y en producción.

## Las dos reglas que no se rompen

1. **Una migración aplicada nunca se edita.** Flyway guarda un checksum de cada archivo; si
   modificas uno ya aplicado, el arranque falla con `Migration checksum mismatch`. ¿Te equivocaste
   en `V3`? Se corrige con `V4`.
2. **Los cambios de datos también son migraciones.** Un `UPDATE` correctivo va en su propio archivo
   versionado, no se ejecuta suelto contra la BD.

## Datos de referencia (seeds)

- **`status_appointment`** → `V2__seed_status_appointment.sql`. Antes vivía en
  `src/main/resources/data.sql` con `spring.sql.init.mode: always`; ese archivo y esa propiedad se
  eliminaron para no tener dos mecanismos de seed en paralelo.
- **Roles y permisos** → los sigue gestionando `DataInitializer` (`ApplicationRunner` en
  `infrastructure.config`), que es idempotente. **No se duplican en SQL**: tener el mismo dato
  sembrado en dos lugares es garantía de divergencia entre entornos.

## Cómo se generó el baseline

`V1__baseline_schema.sql` no se escribió a mano: se exportó el DDL que Hibernate ya venía
aplicando, y luego se reordenó para lectura y se le pusieron nombres de constraint explícitos
(Hibernate los genera tipo `FKmyowslj1th8d9j6j3wlbwrtoe`).

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="\
  -Dspring.jpa.hibernate.ddl-auto=none \
  -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/schema-generado.sql"
```

Ese comando es útil para comparar: si sospechas que una entidad y el schema divergieron, genera el
DDL y contrástalo contra las migraciones. `ddl-auto=none` garantiza que no toque la BD.

Dos detalles que ese export reveló y que a mano se habrían errado: `CustomerEntity` y
`StatusAppointmentEntity` usan `GenerationType.AUTO`, que en Hibernate 6 se resuelve a
**secuencia** con `increment by 50` (`customer_seq`, `status_appointment_seq`), no a `identity`.
