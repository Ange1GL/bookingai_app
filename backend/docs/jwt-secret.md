# Secreto del JWT (HS256)

El access token JWT se firma y se valida con **HS256** (HMAC-SHA256), usando un único secreto simétrico en la
variable de entorno `JWT_SECRET`. El secreto **nunca** va en git ni dentro de la imagen Docker.

## Por qué HS256 y no RS256

La app es un **monolito**: el mismo proceso emite y valida los tokens. La ventaja de RS256 (asimétrico) es que
otros servicios pueden **validar** con la clave pública sin poder **firmar**. Aquí no hay otros servicios, así
que esa ventaja no se aprovecha. HS256 con un secreto aleatorio de 256 bits es igual de seguro y mucho más
simple: un solo valor de una línea, igual en todos los ambientes, sin PEM ni archivos.

Antes se usaba RS256 con llaves PEM (`JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, archivos `certs/*.pem`, `JwtKeyLoader`
y `PemText`). Todo eso se eliminó.

**Cuándo volver a RS256:** si algún día otro servicio necesita validar los tokens (microservicios). En ese caso
el emisor publica sus claves públicas en un endpoint JWKS (`/.well-known/jwks.json`, con un `kid` estable) y los
demás servicios validan con `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`. El cambio queda acotado a
`JwtConfig`: el puerto `TokenService` no cambia.

## Cómo se carga (`JwtConfig` + `JwtSecretKeys`)

| Ambiente | Fuente de `JWT_SECRET` |
|---|---|
| Dev local | `.env` (o variables del sistema / del IDE) |
| QA (ECS Fargate) | Parameter Store (SecureString), inyectado por ECS como variable de entorno |

`application.yaml` declara `security.jwt.secret: ${JWT_SECRET}` **sin valor por defecto**: si falta, la app no
arranca. `JwtSecretKeys.fromBase64` decodifica el base64 y valida al arrancar:

- vacío o ausente → `IllegalStateException` ("no está definido");
- base64 inválido → `IllegalStateException`;
- menos de **32 bytes** decodificados (256 bits, mínimo de HS256 según RFC 7518) → `IllegalStateException`.

Los mensajes de error nunca incluyen el valor del secreto.

`JwtConfig` construye:

- **encoder:** `NimbusJwtEncoder` con el secreto; `JwtTokenService` firma con el header `alg: HS256`.
- **decoder:** `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`. El algoritmo queda fijo, así que un
  token con otro `alg` (`none`, `RS256`, ...) se rechaza. Además valida `exp`, `iss` y `aud`.

## Generar el secreto (uno distinto por ambiente)

Cualquiera de estas opciones produce 32 bytes aleatorios en base64:

```bash
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

```bash
openssl rand -base64 32
```

No uses generadores web: el secreto pasaría por un servidor ajeno.

## Configurarlo en dev

En `backend/.env`, en una sola línea y sin comillas:

```properties
JWT_SECRET=<valor generado>
```

## Guardar en Parameter Store (QA)

```bash
aws ssm put-parameter --region us-east-1 --name JWT_SECRET --type SecureString --value "<valor generado>"
```

(Para cambiar un valor existente agrega `--overwrite`.) `.aws/task-definition-backend.json` lo referencia en
`secrets`. El rol `ecsTaskExecutionRole` necesita `ssm:GetParameters` sobre
`arn:aws:ssm:us-east-1:707256068332:parameter/JWT_SECRET` (con la llave KMS por defecto `aws/ssm` no requiere
permisos KMS adicionales).

**Orden al desplegar un cambio de secreto:** primero crear o actualizar el parámetro y después desplegar. Si el
parámetro no existe, la task de ECS no arranca.

## Rotación

Cambiar el secreto invalida los access tokens emitidos con el anterior. Como viven 15 minutos y los refresh
tokens son opacos (guardados en BD, no son JWT), el frontend recibe un 401, hace refresh y continúa: **nadie
tiene que volver a iniciar sesión**.

Si el secreto llegó a un repositorio, una imagen o un log, considéralo comprometido y rótalo.
