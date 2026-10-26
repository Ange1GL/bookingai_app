# Plan: tutorial de Terraform levantando la arquitectura QA de Aglacita

Fuente: `Downloads/arquitectura-aglacita-qa.md` (CloudFront + S3 + API Gateway HTTP + VPC Link + Cloud Map + ECS Fargate + SSM).

## Idea central: idempotencia y estado deseado

Terraform es **declarativo**: describes *cómo quieres que quede* la infraestructura, no los pasos. Compara
tres cosas: tu código (deseado), el **state** (lo que Terraform cree que existe) y AWS real. `terraform plan`
muestra la diferencia; `terraform apply` la aplica. Ejecutarlo dos veces seguidas no cambia nada
(idempotente). Eso es lo que permite "levantar con un comando" y destruir con otro.

## Decisión importante: no tocar lo que ya existe

En tu cuenta ya corre el QA creado a mano. Si Terraform intenta crear `aglacita-qa-front` chocará con él.
Propuesta: el tutorial crea un **entorno paralelo** con prefijo `aglacita-tf` (misma arquitectura, otros
nombres), así puedes comparar, y destruirlo sin riesgo. Al final, una lección opcional de
`terraform import` para adoptar el QA manual.

Costo del entorno paralelo: ~40 USD/mes encendido, ~1.3 USD/día. Cada lección que lo crea se puede
destruir al terminar con `terraform destroy`.

## Estructura de archivos

```
terraform/
  README.md                  # índice del tutorial
  docs/00..07-*.md           # una lección por fase (concepto -> código -> comandos -> qué observar)
  envs/qa/                   # el "root module" que ejecutas
    versions.tf providers.tf variables.tf main.tf outputs.tf terraform.tfvars.example
  modules/
    static-site/             # S3 privado + OAC + CloudFront
    network/                 # security groups (usa la VPC default como data source)
    api/                     # ECS cluster, task, service, Cloud Map, logs, IAM
    api-gateway/             # HTTP API + VPC Link + integración Cloud Map
    secrets/                 # parámetros SSM
```

## Lecciones (un commit por fase, revertible)

| # | Lección | Conceptos de Terraform | Conceptos de seguridad/AWS |
|---|---|---|---|
| 0 | Preparación | providers, `init`, `plan`, `apply`, `destroy`, el state | credenciales por perfil, nunca claves en código |
| 1 | S3 privado | `resource`, atributos, referencias, `variable`, `output`, `locals` | Block Public Access, cifrado, versionado |
| 2 | CloudFront + OAC | dependencias implícitas, `data` sources, `jsonencode`, políticas administradas | OAC vs bucket público, HTTPS forzado |
| 3 | Red y security groups | `data` (VPC default), `for_each`, reglas mínimas | mínimo privilegio, cerrar el puerto 80 del `default` |
| 4 | Secretos en SSM | `sensitive`, variables por entorno, `.tfvars` fuera de git | SecureString, secretos fuera del state en lo posible |
| 5 | ECS Fargate + Cloud Map + logs | recursos con dependencias, `depends_on`, IAM roles | rol de ejecución mínimo, retención de logs, health check con gracia |
| 6 | API Gateway + VPC Link | integración entre módulos, `outputs` encadenados | proteger `execute-api` con header secreto de CloudFront |
| 7 | Módulos y remote state | `module`, backend S3 + bloqueo, `import`, `moved` | state cifrado y versionado |

Las mejoras de la sección 10 de tu documento (error pages SPA, response headers, WAF, rate limiting) se
incorporan en las lecciones correspondientes en vez de replicar los defectos del QA manual.

## Reglas que respetaré

- Ningún secreto en código ni en git: `.gitignore` para `*.tfstate*`, `*.tfvars`, `.terraform/`.
- `terraform plan` primero y lo leemos juntos; **no ejecuto `apply` sin tu confirmación** en cada lección.
- Documentación (README y CLAUDE.md) actualizada en el mismo commit. Scripts auxiliares en Node `.mjs`.
- La base de datos no está en tu documento; `DB_URL` será un parámetro que tú proporcionas, no creo RDS.

## Preguntas abiertas

1. ¿Entorno paralelo `aglacita-tf` (recomendado) o prefieres adoptar el QA actual con `import`?
2. ¿Idioma de las lecciones: español (asumido)?
3. ¿Perfil de AWS: uso el que está activo ahora (`aws sts get-caller-identity` responde en la cuenta 7072-5606-8332)?
