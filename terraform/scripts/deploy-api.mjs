#!/usr/bin/env node
// Construye la imagen del backend y la sube a ECR con el hash corto del último commit que tocó el backend.
// Uso (desde cualquier carpeta):  node terraform/scripts/deploy-api.mjs
import { execFileSync } from 'child_process'
import { dirname, join, resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const TERRAFORM_DIR = resolve(__dirname, '..')
const QA_DIR = join(TERRAFORM_DIR, 'envs', 'qa')
const BACKEND_DIR = resolve(TERRAFORM_DIR, '..', 'backend')
const REGION = 'us-east-1'

const run = (cmd, args, options = {}) =>
  execFileSync(cmd, args, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'inherit'], ...options }).trim()

const show = (cmd, args, options = {}) =>
  execFileSync(cmd, args, { stdio: 'inherit', ...options })

class DeployError extends Error {}

// Envuelve un paso para que, si falla, el mensaje diga qué revisar en lugar de un stack trace.
function step(hint, action) {
  try {
    return action()
  } catch (error) {
    if (error instanceof DeployError) throw error
    throw new DeployError(`${hint}\n(${error.message.split('\n')[0]})`)
  }
}

function imageExists(repositoryName, tag) {
  try {
    run('aws', ['ecr', 'describe-images', '--region', REGION, '--repository-name', repositoryName, '--image-ids', `imageTag=${tag}`], {
      stdio: ['pipe', 'pipe', 'pipe'],
    })
    return true
  } catch (error) {
    // "No existe" es una respuesta normal; cualquier otro fallo (permisos, red) sí debe verse.
    if (String(error.stderr).includes('ImageNotFoundException')) return false
    throw error
  }
}

// La etiqueta es inmutable: debe corresponder al código del commit, no a cambios sin commitear.
function assertBackendIsClean() {
  const changes = run('git', ['status', '--porcelain', '--', '.'], { cwd: BACKEND_DIR })
  if (changes) {
    throw new DeployError(`Hay cambios sin commitear en backend/. Haz commit antes de desplegar:\n${changes}`)
  }
}

function deploy() {
  const repositoryUrl = step('No se pudo leer ecr_repository_url. ¿Hiciste terraform apply de la lección 5 en envs/qa?', () =>
    run('terraform', ['output', '-raw', 'ecr_repository_url'], { cwd: QA_DIR }),
  )
  const registry = repositoryUrl.split('/')[0]
  const repositoryName = repositoryUrl.split('/').slice(1).join('/')

  step('No se pudo consultar git en backend/.', assertBackendIsClean)
  // Último commit que tocó el backend: cambios solo en terraform/ o docs/ no generan etiqueta nueva.
  const tag = step('No se pudo obtener el commit del backend.', () =>
    run('git', ['log', '-1', '--format=%h', '--', '.'], { cwd: BACKEND_DIR }),
  )
  const image = `${repositoryUrl}:${tag}`

  const exists = step('No se pudo consultar ECR. ¿Tienes credenciales de AWS válidas (aws sts get-caller-identity)?', () =>
    imageExists(repositoryName, tag),
  )

  if (exists) {
    console.log(`La imagen ${image} ya existe en ECR (el repositorio es inmutable): no se vuelve a subir.`)
  } else {
    step('Falló el login en ECR. ¿Credenciales de AWS válidas y Docker abierto?', () => {
      const password = run('aws', ['ecr', 'get-login-password', '--region', REGION])
      run('docker', ['login', '--username', 'AWS', '--password-stdin', registry], { input: password })
    })
    step('Falló docker build. ¿Está Docker abierto? Revisa el error de arriba.', () =>
      show('docker', ['build', '--platform', 'linux/amd64', '-t', image, '.'], { cwd: BACKEND_DIR }),
    )
    step('Falló docker push. Revisa el error de arriba.', () => show('docker', ['push', image]))
  }

  console.log(`\nListo. Pon esto en terraform/envs/qa/terraform.tfvars y haz terraform apply:\n`)
  console.log(`api_image_tag     = "${tag}"`)
  console.log('api_desired_count = 1')
}

try {
  deploy()
} catch (error) {
  if (!(error instanceof DeployError)) throw error
  console.error(`\nError: ${error.message}`)
  process.exit(1)
}
