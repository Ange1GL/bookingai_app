#!/usr/bin/env node
// Carga los secretos de la API en SSM Parameter Store SIN pasar por Terraform ni mostrarlos.
//
//   node scripts/load-ssm-from-env.mjs            # simulacro: dice qué haría
//   node scripts/load-ssm-from-env.mjs --apply    # escribe en AWS
//
// Opciones: --env-file <ruta>  (por defecto ../backend/.env)
//           --prefix <ruta>    (por defecto /aglacita-tf/qa)
//           --force            (reescribe también los parámetros ya cargados)
//
// De dónde sale cada valor: ver COPY_FROM_ENV, GENERATE y FROM_QA_BLOCK más abajo.
import { execFileSync } from 'child_process'
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'fs'
import { tmpdir } from 'os'
import { join, resolve, dirname } from 'path'
import { randomBytes } from 'crypto'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(__dirname, '..')

const PLACEHOLDER = 'CHANGE_ME' // mismo valor que local.ssm_placeholder en main.tf
const PLACEHOLDER_VERSION = 1 // versión de un parámetro que aún tiene el relleno
const JWT_SECRET_BYTES = 32 // HS256 exige >= 32 bytes decodificados

// Se copian tal cual del .env: son credenciales de servicios externos, iguales en QA.
const COPY_FROM_ENV = ['DEEPSEEK_API_KEY', 'API_OPENAI_KEY', 'WHATSAPP_VERIFY_TOKEN', 'WHATSAPP_APP_SECRET']
// Las variables DB_* activas del .env son de desarrollo (localhost) y no sirven en QA.
// Se toman del bloque comentado "Base de datos qa"; si falta, se piden en la terminal.
const FROM_QA_BLOCK = ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD']
// Se genera uno nuevo para QA en vez de reutilizar el de desarrollo.
const GENERATE = ['JWT_SECRET']

const args = process.argv.slice(2)
const flag = (name) => args.includes(name)
const option = (name, fallback) => (args.includes(name) ? args[args.indexOf(name) + 1] : fallback)

const apply = flag('--apply')
const force = flag('--force')
const envFile = resolve(option('--env-file', join(ROOT, '..', 'backend', '.env')))
const prefix = option('--prefix', '/aglacita-tf/qa')

const unquote = (text) => text.trim().replace(/^(['"])(.*)\1$/, '$2')

// Devuelve las variables activas (`values`) y las que están comentadas dentro de un
// encabezado que menciona "qa" (`qaValues`), p. ej.:
//   # Base de datos qa
//   # DB_URL=jdbc:...
// El bloque termina en la primera línea en blanco o en otro comentario que no sea una asignación.
function parseEnv(path) {
  if (!existsSync(path)) throw new Error(`No existe ${path}`)
  const values = {}
  const qaValues = {}
  let inQaBlock = false

  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    const commented = trimmed.startsWith('#')
    const assignment = (commented ? trimmed.slice(1) : trimmed).match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*)$/)

    if (!commented && assignment) values[assignment[1]] = unquote(assignment[2])
    else if (commented && assignment && inQaBlock) qaValues[assignment[1]] = unquote(assignment[2])
    else if (commented) inQaBlock = /\bqa\b/i.test(trimmed)
    else if (trimmed === '') inQaBlock = false
  }
  return { values, qaValues }
}

// Lee sin eco: la contraseña no queda en pantalla ni en el historial de la terminal.
function promptHidden(label) {
  return new Promise((resolvePrompt) => {
    process.stdout.write(`${label}: `)
    let value = ''
    process.stdin.setRawMode(true)
    process.stdin.resume()
    process.stdin.setEncoding('utf8')
    const onData = (char) => {
      if (char === '\r' || char === '\n') {
        process.stdin.setRawMode(false)
        process.stdin.pause()
        process.stdin.off('data', onData)
        process.stdout.write('\n')
        resolvePrompt(value)
      } else if (char === '') {
        process.exit(130)
      } else if (char === '\b' || char === '\x7f') {
        value = value.slice(0, -1)
      } else {
        value += char
      }
    }
    process.stdin.on('data', onData)
  })
}

function aws(...cliArgs) {
  return execFileSync('aws', cliArgs, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
}

// La versión 1 es el relleno de Terraform; cualquier otra ya tiene un valor real.
function parameterVersion(name) {
  try {
    return Number(aws('ssm', 'get-parameter', '--name', name, '--query', 'Parameter.Version', '--output', 'text'))
  } catch (error) {
    throw new Error(`No pude leer ${name}. ¿Ya ejecutaste "terraform apply" y tienes permisos de SSM?\n${error.stderr ?? error.message}`)
  }
}

// El valor va en un archivo temporal (no en argumentos) para que no aparezca en la lista de procesos.
function putSecret(name, value) {
  const dir = mkdtempSync(join(tmpdir(), 'ssm-'))
  const file = join(dir, 'param.json')
  try {
    writeFileSync(file, JSON.stringify({ Name: name, Value: value, Type: 'SecureString', Overwrite: true }), { mode: 0o600 })
    aws('ssm', 'put-parameter', '--cli-input-json', `file://${file}`)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

function validate(key, value) {
  if (!value || value === PLACEHOLDER) throw new Error(`${key}: valor vacío o de relleno`)
  if (key === 'DB_URL' && (!value.startsWith('jdbc:') || /localhost|127\.0\.0\.1/.test(value))) {
    throw new Error('DB_URL: debe ser una URL jdbc: y no apuntar a localhost (la tarea de ECS no la alcanzaría)')
  }
  if (key === 'JWT_SECRET' && Buffer.from(value, 'base64').length < JWT_SECRET_BYTES) {
    throw new Error('JWT_SECRET: debe tener al menos 32 bytes en base64')
  }
}

async function main() {
  const { values: env, qaValues } = parseEnv(envFile)
  const keys = [...COPY_FROM_ENV, ...GENERATE, ...FROM_QA_BLOCK]
  console.log(`${apply ? 'APLICANDO' : 'SIMULACRO'} sobre ${prefix}  (fuente: ${envFile})\n`)

  const describeSource = (key) => {
    if (COPY_FROM_ENV.includes(key)) return 'copiar del .env'
    if (GENERATE.includes(key)) return 'generar nuevo'
    return qaValues[key] ? 'copiar del bloque "qa" del .env' : 'pedir en terminal (no está en el bloque "qa")'
  }

  for (const key of keys) {
    const name = `${prefix}/${key}`
    const source = describeSource(key)

    if (!apply) {
      console.log(`  ${key.padEnd(24)} ${source}`)
      continue
    }
    if (!force && parameterVersion(name) > PLACEHOLDER_VERSION) {
      console.log(`  ${key.padEnd(24)} ya cargado, se omite (usa --force para reescribir)`)
      continue
    }

    let value
    if (COPY_FROM_ENV.includes(key)) value = env[key]
    else if (GENERATE.includes(key)) value = randomBytes(JWT_SECRET_BYTES).toString('base64')
    else value = qaValues[key] ?? (await promptHidden(`  ${key} (QA)`))

    validate(key, value)
    putSecret(name, value)
    console.log(`  ${key.padEnd(24)} cargado (${source})`)
  }
  if (!apply) console.log('\nNo se escribió nada. Agrega --apply para cargar.')
}

main().catch((error) => {
  console.error(`\nError: ${error.message}`)
  process.exit(1)
})
