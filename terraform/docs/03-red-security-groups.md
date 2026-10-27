# Lección 3: red y security groups

Antes de poner cómputo (lección 5) hay que decidir **quién puede hablar con quién**. El código está en
[`../envs/qa/network.tf`](../envs/qa/network.tf) y [`../envs/qa/security_groups.tf`](../envs/qa/security_groups.tf).

```
API Gateway ──► VPC Link ──────────► tarea Fargate (API :8082) ──► Internet (ECR, SSM, DeepSeek, BD)
               vpclink-sg  ──TCP 8082──►  api-sg
```

No se crea VPC: se usa la **VPC por defecto** (subredes públicas, sin NAT Gateway). Es la misma decisión
del QA manual: un NAT cuesta más que todo el cómputo de QA. Lo que sí se crea son dos security groups.

## Conceptos de Terraform

### `data` con filtros

```hcl
data "aws_vpc" "default" { default = true }

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}
```

Un `data` **lee** lo que ya existe y nunca lo crea ni lo destruye: `terraform destroy` no puede dañar la
VPC. Los `filter` son los mismos de `aws ec2 describe-subnets --filters`. Observa el encadenamiento: el
segundo `data` usa el id del primero (dependencia implícita, igual que con los recursos).

### Una regla = un recurso

Se puede declarar un grupo con bloques `ingress { }` / `egress { }` dentro (estilo antiguo), pero entonces
Terraform se vuelve dueño **absoluto** de las reglas y choca con las reglas sueltas; además dos grupos que
se referencian entre sí forman un ciclo. El estilo actual es un grupo vacío más un
`aws_vpc_security_group_ingress_rule` / `_egress_rule` por regla, y se pueden referenciar sin ciclo
(`vpclink` → `api` y `api` → `vpclink`).

### Referenciar un grupo en vez de una IP

```hcl
referenced_security_group_id = aws_security_group.vpclink.id
```

"Permite a cualquiera que lleve este grupo". Las IP de las tareas cambian en cada despliegue; el grupo no.

### `for_each` sobre un conjunto

```hcl
for_each = toset(var.api_debug_cidrs)
cidr_ipv4 = each.value
```

Crea **una regla por elemento**. La clave en el state es el propio CIDR (`...api_debug["203.0.113.7/32"]`),
así que quitar uno de la lista destruye solo esa regla. Con lista vacía no se crea ninguna.
Diferencia con `count`: `count` indexa por posición (`[0]`, `[1]`) y al quitar el primero desplaza y
recrea los demás.

### `count` como interruptor

```hcl
count = var.harden_default_sg ? 1 : 0
```

`count = 0` significa "este recurso no existe". Es el patrón para recursos opcionales. Al usarlo, el
recurso pasa a ser una lista: se referencia como `aws_default_security_group.default[0]`.

### `validation` con funciones

```hcl
condition = alltrue([for cidr in var.api_debug_cidrs : can(cidrnetmask(cidr)) && cidr != "0.0.0.0/0"])
```

`for` genera una lista de booleanos, `can()` convierte un error en `false`, `cidrnetmask()` falla si el
CIDR es inválido y `alltrue()` exige que todos cumplan. Pruébalo con `-var 'api_debug_cidrs=["0.0.0.0/0"]'`.

### Recurso que adopta uno existente

`aws_default_security_group` no **crea** nada: toma el grupo `default` que AWS ya trajo y lo gestiona.
Sin reglas declaradas, deja el grupo vacío. Al destruirlo, Terraform solo lo olvida; no lo puede borrar.

## Seguridad: qué mejora respecto al QA manual

| QA manual | Aquí | Por qué |
|---|---|---|
| `api-sg` aceptaba 8082 desde todo el CIDR `172.31.0.0/16` | Solo desde `vpclink-sg` | Nada más de la VPC necesita llamar a la API |
| `api-sg` aceptaba 8082 desde una IP `/32` fija | Lista `api_debug_cidrs`, vacía por defecto | El acceso de pruebas es explícito y temporal |
| `vpclink-sg` salía a `0.0.0.0/0` | Sale solo a `api-sg:8082` | Si el VPC Link se compromete, solo llega a la API |
| `default` con TCP 80 abierto a Internet | `harden_default_sg` lo vacía (opcional) | Era la regla más expuesta del entorno |

La salida de `api-sg` sigue abierta (`-1` a `0.0.0.0/0`) porque la tarea está en subred pública sin NAT y
necesita alcanzar ECR, SSM, DeepSeek y la base de datos. Es una concesión consciente; en producción se
resolvería con subredes privadas y VPC endpoints.

### Por qué `harden_default_sg` está apagado

La VPC por defecto es **compartida** con tu QA manual y el documento de arquitectura dice que no se
verificó qué recursos usan el grupo `default`. Vaciarlo podría cortar tráfico entre recursos que se
confían entre sí mediante la regla "todo desde sí mismo". Antes de activarlo:

```bash
aws ec2 describe-network-interfaces \
  --filters Name=group-id,Values=<id-del-grupo-default> \
  --query "NetworkInterfaces[].[NetworkInterfaceId,Description]" --output table
```

Si la tabla sale vacía, es seguro vaciarlo. Si hay interfaces, ese recurso lo usa.

## Permisos de IAM necesarios

Crea una política nueva (`terraform-aglacita-tf-network`) y adjúntala a `angel`:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "TerraformNetworkRead",
			"Effect": "Allow",
			"Action": [
				"ec2:DescribeVpcs",
				"ec2:DescribeVpcAttribute",
				"ec2:DescribeSubnets",
				"ec2:DescribeSecurityGroups",
				"ec2:DescribeSecurityGroupRules",
				"ec2:DescribeNetworkInterfaces",
				"ec2:DescribeTags"
			],
			"Resource": "*"
		},
		{
			"Sid": "TerraformSecurityGroups",
			"Effect": "Allow",
			"Action": [
				"ec2:CreateSecurityGroup",
				"ec2:DeleteSecurityGroup",
				"ec2:AuthorizeSecurityGroupIngress",
				"ec2:AuthorizeSecurityGroupEgress",
				"ec2:RevokeSecurityGroupIngress",
				"ec2:RevokeSecurityGroupEgress",
				"ec2:ModifySecurityGroupRules",
				"ec2:UpdateSecurityGroupRuleDescriptionsIngress",
				"ec2:UpdateSecurityGroupRuleDescriptionsEgress",
				"ec2:CreateTags",
				"ec2:DeleteTags"
			],
			"Resource": "*"
		}
	]
}
```

Los `Describe*` no admiten restringir por recurso. Para las acciones de escritura se podría acotar por
ARN o por etiqueta, pero un id de grupo no existe hasta crearlo; es la misma limitación de CloudFront.
Si Terraform pide otro permiso, el error dice cuál.

## Ejecutar

```bash
cd terraform/envs/qa
terraform plan
```

Esperado (con las lecciones 1 y 2 ya aplicadas): `Plan: 5 to add, 0 to change, 0 to destroy.`
Son dos grupos (`vpclink`, `api`) y tres reglas (`vpclink_to_api`, `api_from_vpclink`, `api_all_out`).
Los `data` no cuentan porque no crean nada, y `api_debug` y el grupo `default` valen cero por defecto.

Si el `plan` falla con `UnauthorizedOperation ... ec2:DescribeVpcs`, es que falta la política de IAM de
arriba: adjúntala y repite.

```bash
terraform apply     # escribe yes
```

Los security groups se crean en segundos. Los dos grupos van primero (en paralelo) y las reglas después,
porque se referencian entre sí: ese es el grafo de dependencias en acción.

## Qué observar

1. Outputs: `vpc_id`, `subnet_ids`, `vpclink_security_group_id`, `api_security_group_id`.
2. En la consola, EC2 → Security Groups: `aglacita-tf-qa-api-sg` solo tiene **una** regla de entrada, con
   origen `sg-...` (el VPC Link), no una IP.
3. `terraform state list`: las reglas de depuración no aparecen (lista vacía).
4. Prueba la validación: `terraform plan -var 'api_debug_cidrs=["0.0.0.0/0"]'` falla antes de tocar AWS.
5. Agrega tu IP: `terraform plan -var 'api_debug_cidrs=["203.0.113.7/32"]'` muestra **una** regla nueva con
   clave `["203.0.113.7/32"]`.

## Limpiar

```bash
terraform destroy
```

Los security groups no cuestan nada, pero `destroy` fallaría si una interfaz de red aún los usa (por
ejemplo, el VPC Link o la tarea de lecciones posteriores). Se destruyen primero esos recursos; Terraform
respeta el orden inverso del grafo.

## Ejercicios

1. Cambia `api_port` a `9090` y ejecuta `plan`: cambian las dos reglas que usan el puerto
   (`vpclink_to_api` y `api_from_vpclink`) y los grupos no se tocan. ¿Se actualizan in situ (`~`) o se
   reemplazan (`-/+`)? ¿Por qué no hay que editar dos sitios?
2. Pasa `api_debug_cidrs=["203.0.113.7/32","198.51.100.0/24"]`, aplica, y luego deja solo el segundo.
   Comprueba en el `plan` que solo se destruye la primera regla.
3. Restringe `api_all_out` a `tcp/443` y al puerto de tu base de datos con dos reglas. ¿Qué dejaría de
   funcionar si olvidas el 443?
4. Desafío: verifica con la consulta de arriba qué usa el grupo `default` y, si está libre, activa
   `harden_default_sg=true` y lee el `plan`. Dirá `1 to add` aunque el grupo ya existe: ¿por qué? (Pista:
   "crear" un recurso de tipo "adopta" significa tomar el existente y dejarlo como lo declaras.)

Siguiente: Lección 4, secretos en SSM.
