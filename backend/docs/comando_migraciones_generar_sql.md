# Comandos para migraciones: generar SQL, limpiar la BD y arrancar Flyway

Referencia práctica de los comandos que se usan alrededor de Flyway en este proyecto, qué hace
cada uno, desde dónde se ejecuta y qué esperar en la salida. Complementa a `flyway-migrations.md`
(que explica el *por qué* del diseño); este archivo es el *cómo*, para copiar y pegar.

Todos los comandos se ejecutan en **PowerShell**, desde la carpeta donde está el `pom.xml`:

```powershell
cd C:\Users\RAM\projects_personal\java_leetcode\app_springs\bookingapp\bookingapp\backend
```

---

## 0. Cómo se ejecutan estos comandos (no hay nada "instalado")

No existe un ejecutable `flyway` en la máquina. Lo único instalado es el **JDK 25**. Todo lo
demás lo descarga Maven bajo demanda a `C:\Users\RAM\.m2\repository\` la primera vez que se pide.

| Python | Java / este proyecto |
|---|---|
| Intérprete `python` instalado | JVM (JDK 25) — lo único instalado de verdad |
| `pip install flyway` | Maven descarga el `.jar` la primera vez |
| `python -m flyway clean` | `.\mvnw.cmd org.flywaydb:flyway-maven-plugin:11.14.1:clean` |

Y ni siquiera Maven está instalado: `mvnw.cmd` es el *Maven Wrapper*, un script del repo que
descarga la versión exacta de Maven la primera vez y luego la invoca. Por eso quien clone el
proyecto solo necesita el JDK.

### Anatomía del comando del plugin

```
.\mvnw.cmd  org.flywaydb : flyway-maven-plugin : 11.14.1 : clean
    │            │               │                 │        │
    │            │               │                 │        └─ goal: qué acción ejecutar
    │            │               │                 └────────── versión exacta a descargar
    │            │               └──────────────────────────── artifactId: el nombre del plugin
    │            └──────────────────────────────────────────── groupId: quién lo publica
    └─────────────────────────────────────────────────────── el wrapper que arranca Maven sobre la JVM
```

Se escribe con el nombre completo porque el plugin **no está declarado en el `pom.xml`**; si lo
estuviera, bastaría `.\mvnw.cmd flyway:clean`.

Hay que correrlo desde la carpeta del `pom.xml` porque el plugin toma de ahí el **driver de
PostgreSQL** y **`flyway-database-postgresql`** para su classpath.

### Flyway corre de dos maneras distintas

| | Cuándo corre | Quién lo arranca | Qué configuración lee |
|---|---|---|---|
| `flyway-core` dentro de la app | En cada `spring-boot:run` | Spring Boot (vía `spring-boot-flyway`) | `application.yaml` + `.env` (el `datasource`) |
| `flyway-maven-plugin` | Solo cuando se invoca a mano | Maven | Variables `FLYWAY_*` o flags `-Dflyway.*` |

El primero **migra** (aplica lo pendiente al arrancar). El segundo **administra** desde fuera:
`clean`, `info`, `repair`, `baseline`. Son dos jars distintos con el mismo motor.

---

## 1. Variables de entorno para el plugin

El plugin **no es Spring Boot**: no lee `application.yaml`, no entiende `${DB_URL}` y no abre el
`.env` por su cuenta. Solo mira las variables de entorno reales del proceso, con nombres `FLYWAY_*`.

Por eso el `.env` tiene, además de las `DB_*`, estas cuatro líneas (mismos valores):

```properties
FLYWAY_URL=<igual que DB_URL>
FLYWAY_USER=<igual que DB_USERNAME>
FLYWAY_PASSWORD=<igual que DB_PASSWORD>
FLYWAY_CLEAN_DISABLED=false
```

- `FLYWAY_CLEAN_DISABLED=false` habilita `clean`. Flyway lo trae **bloqueado por defecto** como
  seguro contra borrar producción; aquí se desbloquea porque el `.env` apunta a la BD de
  desarrollo local. Si ese archivo alguna vez apunta a una base que importe, quitar esta línea.
- La contraseña queda **dos veces** en el `.env` (`DB_PASSWORD` y `FLYWAY_PASSWORD`) porque el
  formato `.env` no permite `FLYWAY_PASSWORD=${DB_PASSWORD}`. Si cambia la contraseña, actualizar
  ambas. El `.env` está en `.gitignore`; nada de esto se comitea.

### Cargar el `.env` en la sesión de PowerShell

Un archivo `.env` es solo texto; PowerShell no lo lee solo. Hay que cargarlo **una vez por
ventana** (si se cierra la ventana, se repite):

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item "env:$($k.Trim())" $v.Trim() }
```

Lee cada `CLAVE=valor` (ignora comentarios) y la convierte en variable de entorno de esa sesión.
Para comprobar que cargó:

```powershell
$env:FLYWAY_URL
```

Debe imprimir `jdbc:postgresql://...`. Si sale vacío, la línea anterior no corrió en esa ventana.

---

## 2. `flyway:clean` — vaciar el schema de desarrollo

Borra **todos** los objetos del schema `public`: tablas, secuencias, constraints y la propia
`flyway_schema_history`. Es la forma correcta de "resetear" la BD de desarrollo.

```powershell
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:11.14.1:clean
```

Éxito: `Successfully cleaned schema "public"` y `BUILD SUCCESS`.

**Por qué `clean` y no borrar tablas a mano:** si se borran tablas manualmente suelen quedar
objetos sueltos (secuencias, la tabla de historial, o al revés). Flyway entonces ve *schema no
vacío + sin historial* y se detiene con:

```
Found non-empty schema(s) "public" but no schema history table.
Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
```

Es una protección deliberada: no asume si la base es nueva o existente, y obliga a decidir:

| Situación | Qué hacer |
|---|---|
| Base nueva, lo que hay es basura | `flyway:clean` → schema vacío → Flyway construye desde `V1` |
| Base existente con datos a conservar | `spring.flyway.baseline-on-migrate=true` → marca lo existente como aplicado |

**Nunca borrar solo `flyway_schema_history` a mano**: eso deja exactamente el estado que Flyway rechaza.

**Nunca correr `clean` fuera de desarrollo.**

### Alternativa sin plugin: desde el gestor de BD

Si tienes pgAdmin, DBeaver o IntelliJ conectados, el equivalente a `clean` es una sola sentencia
y no necesita ni plugin ni variables `FLYWAY_*`:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

El plugin es útil cuando no hay cliente de BD a la mano o para automatizar; para un reset manual
en dev, esta opción es más corta.

---

## 3. `flyway:info` — ver el estado del historial

```powershell
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:11.14.1:info
```

Lista cada versión con `Success` / `Pending`. Útil antes de arrancar para saber qué va a aplicar.
Requiere las variables cargadas (sección 1).

---

## 4. Generar el SQL a partir de las entidades (Hibernate, no Flyway)

Este comando **no es de Flyway**. Es Hibernate comparando las `@Entity` contra la BD real y
escribiendo a un archivo el DDL que falta. Es lo más parecido a `makemigrations` de Django que hay
sin instalar herramientas: **un borrador que se revisa**, no una migración lista.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.jpa.hibernate.ddl-auto=none -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=update -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/diff.sql"
```

Qué hace cada flag:

| Flag | Para qué |
|---|---|
| `ddl-auto=none` | Hibernate **no toca la BD** ni valida. Solo en esta corrida; `application.yaml` sigue en `validate`. |
| `scripts.action=update` | Compara entidades vs. BD y genera solo los `ALTER`/`CREATE` que faltan. En una BD vacía sale **todo el schema**. |
| `scripts.create-target=target/diff.sql` | Dónde escribe el resultado. |

Todo el argumento `-D...` va **entre comillas** para que PowerShell no lo parta.

Cómo se comporta:

- La app **se queda corriendo**. Al ver `Started BookingappApplication`, el archivo ya existe:
  revisar `target\diff.sql` y detener con `Ctrl+C`.
- Orden interno: Flyway corre primero (aplica lo pendiente) → Hibernate construye el
  `EntityManagerFactory` y **ahí escribe el archivo** → arranca Tomcat → corre `DataInitializer`.
- **Si la BD está vacía y no hay migraciones, la app truena al final** con
  `relation "permissions" does not exist`: es `DataInitializer` intentando sembrar roles en tablas
  que no existen. Para ese momento `target\diff.sql` **ya está escrito**. El error es esperado.
- **Si `diff.sql` sale con 0 bytes, es buena señal**: entidades y schema coinciden, no hay
  migración pendiente.

Qué revisar antes de copiar el contenido a un `V__.sql`:

- Viene **una tabla por línea**. Reformatear a una columna por línea; es código, debe ser legible.
- Las FK y UNIQUE ya salen con nombre legible (`fk_appointment_customer`, `uk_user_app_email`)
  porque las entidades los declaran con `@ForeignKey(name = ...)` y `@UniqueConstraint(name = ...)`
  (ver convención en `CLAUDE.md`). Si aparece un `FK<hash>` o `UK_<hash>`, es una relación o
  unique nueva a la que le faltó el nombre en la entidad — corregirlo ahí, no en el SQL.
- Hibernate **nunca genera `DROP` ni `RENAME`**. Un renombre de columna aparece como "columna
  nueva" y deja la vieja huérfana. Ese `ALTER TABLE ... RENAME COLUMN` se escribe a mano.
- Hibernate **nunca genera datos**. Los seeds (`INSERT`) y los backfills (`UPDATE ... SET ...`)
  siempre se escriben a mano.
- Fijarse en `create sequence customer_seq ... increment by 50`: `GenerationType.AUTO` en
  Hibernate 6 es secuencia con optimizador pooled, no `identity`. Es el tipo de detalle por el que
  conviene partir del generador y no escribir el baseline de memoria.

---

## 5. Convención de nombres de los archivos

```
V1__baseline_schema.sql
│││ │              │
││└─┴── DOS guiones bajos (con uno solo, Flyway lo ignora sin avisar)
│└───── versión: 1, 2, 3… Flyway ordena numéricamente
└────── V = versioned, corre una sola vez
        └──────────────── descripción; los guiones bajos se muestran como espacios en el historial
```

Van en `src\main\resources\db\migration\` (default de Spring Boot; no hay que configurar
`spring.flyway.locations`). Nunca reusar un número: dos `V3__` → `Found more than one migration
with version 3`.

---

## 6. Flujo completo: arrancar Flyway en una base virgen (a mano)

Este es el proceso para partir de cero, con carpeta de migraciones vacía y BD vacía.

**1. Borrar migraciones previas** (solo si son borradores sin comitear ni aplicar en otra base):

```powershell
Remove-Item src\main\resources\db\migration\*.sql
```

**2. Vaciar la BD** (sección 1 para cargar variables, luego):

```powershell
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:11.14.1:clean
```

**3. Generar el DDL completo** (sección 4). La app tronará al final por `DataInitializer`; el
`target\diff.sql` ya está escrito.

**4. Crear `V1` a mano** con `diff.sql` como referencia:

```powershell
Get-Content target\diff.sql
New-Item src\main\resources\db\migration\V1__baseline_schema.sql
```

**5. Crear `V2` con los seeds** (el diff no trae datos). `status_appointment` es un catálogo que
refleja el enum `domain.model.StatusAppointment`; el mapeo es por `status_id` (`fromId`), así que
los ids deben coincidir con los del enum. Son **4** estados (el `data.sql` viejo solo tenía 3 —
`FINALIZED`=4 nunca estuvo en la BD):

```sql
INSERT INTO status_appointment (status_id, nombre) VALUES (1, 'PENDING')     ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (2, 'CANCELLED')   ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (3, 'IN_PROGRESS') ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (4, 'FINALIZED')   ON CONFLICT DO NOTHING;
```

Regla: **cada valor nuevo en el enum lleva su `INSERT` en una migración nueva.** Un valor en el
enum sin fila en la tabla viola `fk_appointment_status` en cuanto una cita lo use.

Los roles y permisos **no** van aquí: los siembra `DataInitializer`. No duplicarlos.

> **Escribir el contenido ANTES de arrancar la app.** Un `V__.sql` vacío se aplica como migración
> válida: no falla, no hace nada, y queda registrado con checksum `0`. Al pegarle el SQL después y
> arrancar, Flyway compara y se detiene:
>
> ```
> Migration checksum mismatch for migration version 2
> -> Applied to database : 0
> -> Resolved locally    : -621712970
> ```
>
> Ese `0` es la firma de "se aplicó vacío". Solución en dev: `flyway:clean` y arrancar de nuevo.
> **No usar `repair`** aquí: `repair` alinea el checksum del historial con el archivo pero **no
> ejecuta el SQL** — la base quedaría sin las filas (o sin las tablas, si fue el V1) y Hibernate
> tronaría después con `missing table`. `repair` es para migraciones que sí se ejecutaron y solo
> cambiaron algo cosmético.

**6. Arrancar normal:**

```powershell
.\mvnw.cmd spring-boot:run
```

`application.yaml` no se toca en ningún paso: `ddl-auto: validate` se queda.

---

## 7. Flujo diario: agregar un cambio

1. Modificar la `@Entity`.
2. Generar el diff (sección 4). Ahora sí sale solo lo nuevo, porque Flyway aplicó lo anterior antes
   de que Hibernate compare.
3. Revisar `target\diff.sql` y completar lo que Hibernate no genera (renombres, backfills).
4. Crear `V<siguiente>__<descripcion>.sql` y pegar el SQL revisado. Verificar que el número no exista.
5. Arrancar. Confirmar `Migrating schema "public" to version "<n> - ..."`.
6. Commit de la entidad **y** la migración juntas.

---

## 8. El semáforo del log al arrancar

En este orden:

1. `Successfully validated N migrations` → los checksums coinciden.
2. `Schema "public" is up to date` **o** `Migrating schema ... to version "X"` → aplicó lo nuevo.
   (En base vacía, antes aparece `Creating Schema History table`.)
3. `Initialized JPA EntityManagerFactory` → Hibernate validó: entidades y tablas coinciden.
   **Este es el examen real de un `V__` escrito a mano.**
4. `Roles and permissions initialized.` → `DataInitializer` sembró.
5. `Started BookingappApplication`.

| Falla en | Significa |
|---|---|
| Paso 1, `Migration checksum mismatch` | Se editó una migración ya aplicada. Se corrige con una migración nueva, no editando. Si `Applied to database : 0`, el archivo se aplicó vacío: en dev, `flyway:clean`. |
| Paso 2, `Found non-empty schema` | Se borraron tablas a mano. Usar `flyway:clean`. |
| Paso 3, `missing table` / `missing column` / `wrong column type` | Se cambió una entidad sin su migración, o el `V__` a mano tiene un error. El mensaje dice cuál. |

---

## 9. Lo que NO se hace

| No hacer | Por qué |
|---|---|
| Editar un `V__.sql` ya aplicado | Checksum mismatch; la app no levanta. Corregir con `V<n+1>`. |
| Borrar los `V__.sql` de `db/migration/` | Son la **definición** del schema, código fuente. Sin ellos una base vacía no se puede construir y `validate` truena. Solo se borran borradores no comiteados ni aplicados. |
| Borrar tablas a mano en dev | Deja objetos sueltos; Flyway se detiene. Usar `flyway:clean` o `DROP SCHEMA ... CASCADE`. |
| Borrar solo `flyway_schema_history` | Es exactamente el estado que Flyway rechaza. |
| Volver a `ddl-auto: update` | Hibernate modifica la BD fuera del historial; Flyway y la BD dejan de contar la misma historia. |
| `ALTER`/`UPDATE` sueltos en pgAdmin/DBeaver | Cambios que existen en una BD pero en ningún archivo. No existirán en otra máquina ni en producción. |
| Un solo guion bajo (`V3_algo.sql`) | Flyway lo ignora sin avisar. |
| Copiar `diff.sql` sin leerlo | No trae `DROP`, `RENAME` ni datos. |
| `flyway:clean` fuera de dev | Borra todo el schema. |
