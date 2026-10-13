# Llaves RSA del JWT (dev vs QA)

El JWT se firma con **RS256** (llave privada) y se valida con la llave pública. La clave privada **nunca**
va en git ni dentro de la imagen Docker.

## Cómo se cargan (`JwtConfig` + `JwtKeyLoader`)

| Entorno | Fuente | Propiedades |
|---|---|---|
| Dev local | Texto PEM en `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` (en `.env`, variables de Windows o del IDE) | `security.jwt.private-key-pem` / `public-key-pem` en `application.yaml` (vacías por defecto) |
| Dev local (respaldo) | Si no hay texto: archivos `src/main/resources/certs/private.pem` (ignorado por git) y `public.pem` | `security.jwt.private-key` / `public-key` (por defecto `classpath:certs/*.pem`, sobreescribibles con `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH`) |
| QA (ECS Fargate) | Texto PEM en variables de entorno, inyectadas desde Parameter Store | `security.jwt.private-key-pem` = `${JWT_PRIVATE_KEY}`, `security.jwt.public-key-pem` = `${JWT_PUBLIC_KEY}` (en `application-qa.yaml`, sin valor por defecto: si faltan, la app no arranca) |

ECS solo inyecta secrets como **variables de entorno** (texto), no como archivos. Por eso `JwtKeyLoader`
parsea el texto PEM con `RsaKeyConverters.pkcs8()` / `x509()` de Spring Security; si el texto está vacío usa el
recurso del classpath.

### Formato del texto PEM (`PemText`)

El decoder de Spring exige saltos de línea reales entre encabezado, cuerpo y pie. Como cada origen entrega el
texto distinto, `PemText` lo normaliza antes de parsearlo (reconstruye encabezado + base64 en líneas de 64 + pie).
Acepta:

- Saltos reales (Parameter Store, variables de Windows).
- `\n` literales en una sola línea (`.env`, plugin EnvFile de IntelliJ, que no los convierte).
- `\r\n` de Windows, comillas alrededor, todo en una línea separado por espacios o solo el cuerpo base64.

En el `.env` la llave va en una sola línea con `\n`:

```properties
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\nMIIB...\n-----END PUBLIC KEY-----
```

Si el texto no es base64 válido, la app no arranca y el error indica qué llave falló y si venía del texto o del
archivo de respaldo.

## Generar el par de claves (QA/producción: uno distinto por entorno)

```bash
openssl genrsa -out keypair.pem 2048
openssl rsa -in keypair.pem -pubout -out public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out private.pem
rm keypair.pem
```

`private.pem` debe quedar en PKCS#8 (`BEGIN PRIVATE KEY`) y `public.pem` en X.509 (`BEGIN PUBLIC KEY`).
Los archivos deben ser UTF-8 (en Windows PowerShell evita `>`/`Out-File` por defecto: generan UTF-16).

## Guardar en Parameter Store (SecureString, nivel Standard: sin costo)

```bash
aws ssm put-parameter --region us-east-1 --name JWT_PRIVATE_KEY --type SecureString --value file://private.pem
aws ssm put-parameter --region us-east-1 --name JWT_PUBLIC_KEY  --type SecureString --value file://public.pem
```

(Para cambiar un valor existente agrega `--overwrite`.) Una clave RSA de 2048 bits (~1.7 KB) cabe en el
límite de 4 KB del nivel Standard.

`.aws/task-definition-backend.json` referencia ambos parámetros en `secrets`. El rol `ecsTaskExecutionRole`
necesita `ssm:GetParameters` sobre `arn:aws:ssm:us-east-1:707256068332:parameter/JWT_PRIVATE_KEY` y
`.../JWT_PUBLIC_KEY` (con la llave KMS por defecto `aws/ssm` no requiere permisos KMS adicionales).

## Rotación

Cambiar el par invalida todos los tokens emitidos con el anterior (los usuarios deben iniciar sesión de nuevo).
Si una clave privada llegó a una imagen o repositorio, considérala comprometida y rótala.
