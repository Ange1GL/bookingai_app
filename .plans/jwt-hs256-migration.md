# Plan: migrar JWT de RS256 (RSA) a HS256 (secreto simétrico)

**Estado:** implementado (pendientes: pasos manuales de AWS, sección 5)
**Rama:** `refactor_auth_to_HS256`
**Motivo:** la app es un monolito, así que la separación firmar/verificar de RSA no se aprovecha. HS256 deja
una sola variable (`JWT_SECRET`) igual en todos los ambientes, sin PEM ni archivos de respaldo.

---

## ⚠️ Decisión previa: base de la rama

`refactor_auth_to_HS256` sale de `main` (`9005447`) y **no incluye** los commits de `test_price_catalog`:

- `bf20d17`: agrega `PemText.java` y cambia `JwtKeyLoader`
- `c55d879` / `2b87059`: tests de price catalog
- `2e02f80`: CLAUDE.md

Si migro aquí y luego se mergea `test_price_catalog`, **vuelven `PemText.java` y el `JwtKeyLoader` nuevo**, y
hay conflicto modify/delete.

| Opción | Qué implica |
|---|---|
| **A (recomendada)** | Mergear primero el PR de `test_price_catalog` a `main` y luego hacer `git merge main` en esta rama. El refactor borra `PemText` y `JwtKeyLoader` de una vez |
| B | Mergear `test_price_catalog` en esta rama ahora (`git merge test_price_catalog`) |
| C | Migrar sobre `main` tal cual. Habrá que resolver el conflicto cuando entre `test_price_catalog` |

---

## 1. Configuración

### `backend/src/main/resources/application.yaml`
Reemplazar las 4 propiedades de llaves por una sola, **sin valor por defecto**. Si falta, la app no arranca en
ningún ambiente:

```yaml
security:
  jwt:
    # Secreto HS256 en base64 (>= 32 bytes decodificados). Dev: .env; QA: Parameter Store. Ver docs/jwt-secret.md
    secret: ${JWT_SECRET}
    expiration: 15
    ...
```

Se eliminan `private-key-pem`, `public-key-pem`, `private-key` y `public-key`.

### `backend/src/main/resources/application-qa.yaml`
Eliminar el bloque `security.jwt.private-key-pem/public-key-pem`. QA usa la misma propiedad base; ECS inyecta
`JWT_SECRET`.

---

## 2. Código

### `infrastructure/config/JwtSecretKeys.java` (nuevo, reemplaza a `JwtKeyLoader`)
Clase `final` package-private con una sola responsabilidad: convertir el texto base64 en `SecretKey` y
validarlo en el arranque (es entrada del sistema):

- `null`/en blanco → `IllegalStateException("JWT_SECRET no está definido")`
- base64 inválido → `IllegalStateException` con causa
- menos de `MIN_SECRET_BYTES = 32` (256 bits, lo que exige HS256) → `IllegalStateException` indicando el tamaño
- OK → `new SecretKeySpec(bytes, "HmacSHA256")`

Nunca imprime el secreto en mensajes ni logs.

### `infrastructure/config/JwtConfig.java`
- Quitar los 4 `@Value` de llaves, los beans `RSAPrivateKey`/`RSAPublicKey`, el bean `JWKSet` con
  `RSAKey` y el `kid` aleatorio.
- Nuevo `@Value("${security.jwt.secret}")` y bean `SecretKey jwtSecretKey()` vía `JwtSecretKeys.fromBase64(...)`.
- Encoder: `new NimbusJwtEncoder(new ImmutableSecret<>(secretKey))`
- Decoder: `NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()`.
  El algoritmo queda fijo (rechaza `none` o RS*).
- Se **mantienen sin cambios** los validadores `JwtTimestampValidator`, `JwtIssuerValidator` y
  `JwtAudienceValidator`, y sus comentarios (se ajusta la mención a "firma RS256" → "firma HS256").

### `infrastructure/security/filter/JwtTokenService.java`
- `JwsHeader.with(SignatureAlgorithm.RS256)` → `JwsHeader.with(MacAlgorithm.HS256)`
- Ajustar imports. No cambia nada más: claims, `jti` y manejo de errores quedan igual.

### Sin cambios
`TokenService` (puerto), `JwtAuthenticationFilter`, `AuthTokenIssuer`, refresh tokens, cookies, revocación.
La arquitectura hexagonal aísla el cambio en infraestructura.

### Dependencias (`pom.xml`)
Sin cambios: `spring-boot-starter-oauth2-resource-server` y `nimbus-jose-jwt` soportan HS256.

---

## 3. Archivos que se ELIMINAN (listados para confirmar)

| Archivo | Estado en git |
|---|---|
| `backend/src/main/java/.../infrastructure/config/JwtKeyLoader.java` | versionado |
| `backend/src/test/java/.../infrastructure/config/JwtKeyLoaderTest.java` | versionado |
| `backend/src/main/resources/certs/public.pem` | versionado |
| `backend/src/main/resources/certs/private.pem` | **local, ignorado por git**: se borra del disco (no hay copia en git) |
| `backend/src/main/resources/certs/` | carpeta queda vacía y se borra |
| `PemText.java` (+ su test si existe) | solo si se elige la opción A o B |

`backend/.gitignore`: quitar la línea `src/main/resources/certs/private.pem` y su comentario; se dejan
`*.key`, `*.p12`, etc.

---

## 4. Tests

- **Nuevo** `JwtSecretKeysTest`: secreto válido de 32 bytes → `SecretKey` HmacSHA256; vacío/null, base64
  inválido y secreto de 31 bytes → `IllegalStateException`; el mensaje no contiene el secreto.
- **Nuevo** `JwtTokenServiceTest` (sin Spring): encoder/decoder reales con un secreto generado en el test.
  Casos:
  - genera → decodifica (username, `jti`, `exp`);
  - token firmado con **otro** secreto → `TOKEN_INVALID`;
  - token con `iss`/`aud` distinto → rechazado;
  - header `alg` alterado → rechazado.
- Correr `./mvnw test` (los tests unitarios; `BookingappApplicationTests` requiere BD y `.env`).

---

## 5. Infraestructura (AWS)

### `.aws/task-definition-backend.json`
Reemplazar los secrets `JWT_PRIVATE_KEY` y `JWT_PUBLIC_KEY` por:
```json
{ "name": "JWT_SECRET", "valueFrom": "arn:aws:ssm:us-east-1:707256068332:parameter/JWT_SECRET" }
```

### Pasos manuales tuyos (yo no ejecuto nada en AWS), **en este orden**
1. Generar el secreto (ver sección 7) y crear el parámetro **antes** del deploy; si no, la task no arranca:
   ```bash
   aws ssm put-parameter --region us-east-1 --name JWT_SECRET --type SecureString --value "<secreto>"
   ```
2. Dar permiso `ssm:GetParameters` sobre `.../parameter/JWT_SECRET` al *execution role* de ECS.
3. Mergear y desplegar.
4. Verificar login y `/actuator/health`.
5. Cuando todo funcione, borrar los parámetros viejos `JWT_PRIVATE_KEY` y `JWT_PUBLIC_KEY` y quitarlos de la
   política IAM.

**Impacto en usuarios:** los access tokens vivos (≤ 15 min) dejan de validar. El frontend recibe 401, hace
refresh (los refresh tokens son opacos en BD, no JWT) y continúa. **Nadie tiene que volver a iniciar sesión.**

**Rollback:** redeploy de la revisión anterior de la task definition. Por eso los parámetros viejos se borran
hasta el paso 5.

---

## 6. `.env` local (no versionado)
Quitar `JWT_PRIVATE_KEY` y `JWT_PUBLIC_KEY` y agregar `JWT_SECRET=` vacío con un comentario. Tú pegas el valor
generado; yo no genero ni escribo secretos en archivos.

---

## 7. Generar el secreto (lo entrego en el chat, no como archivo del proyecto)
- **Python:** `python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"`
- **OpenSSL:** `openssl rand -base64 32`
- Cómo setearlo como variable de entorno en PowerShell, bash y el `.env`.
- **No recomiendo generadores web:** el secreto pasaría por un servidor ajeno.

Uno distinto por ambiente (dev ≠ QA).

---

## 8. Documentación (en el mismo commit)

| Archivo | Cambio |
|---|---|
| `backend/docs/jwt-keys.md` | **Renombrar** a `backend/docs/jwt-secret.md` y reescribir: HS256, `JWT_SECRET`, generación, Parameter Store, rotación, por qué HS256 en un monolito y cuándo volver a RS256 (microservicios/JWKS) |
| `backend/README.md` | Quitar la sección "3. Claves RSA" (OpenSSL, PKCS#8, `BadPaddingException`, rutas `JWT_*_KEY_PATH`). Agregar `JWT_SECRET` a variables de entorno; quitar OpenSSL de prerrequisitos; RS256 → HS256 en la descripción |
| `CLAUDE.md` (raíz) | Reemplazar la línea de `jwt-keys.md` por `jwt-secret.md` con el resumen HS256 |
| `backend/docs/security-config.md`, `jwt-authentication-filter.md` | Revisar y ajustar si mencionan RS256 o llaves |

---

## 9. Commit
```
refactor(auth)!: migra firma jwt de rs256 a hs256

La app es un monolito: la separacion firmar/verificar de RSA no se
aprovecha y la carga de PEM (texto + archivo de respaldo) agregaba
complejidad. HS256 usa un solo secreto JWT_SECRET en todos los ambientes.

BREAKING CHANGE: se requiere la variable JWT_SECRET (base64, >= 32 bytes);
JWT_PRIVATE_KEY, JWT_PUBLIC_KEY y JWT_*_KEY_PATH ya no se usan.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

## Preguntas para aprobar
1. ¿Base de la rama: **A**, B o C?
2. ¿OK con eliminar los archivos de la sección 3, incluida tu `private.pem` local?
3. ¿OK con renombrar `jwt-keys.md` → `jwt-secret.md`?
