#!/usr/bin/env node
// Construye la imagen del backend y la sube a ECR con el hash corto del commit como etiqueta.
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

const repositoryUrl = run('terraform', ['output', '-raw', 'ecr_repository_url'], { cwd: QA_DIR })
const [registry, repositoryName] = [repositoryUrl.split('/')[0], repositoryUrl.split('/').slice(1).join('/')]
const tag = run('git', ['rev-parse', '--short', 'HEAD'], { cwd: BACKEND_DIR })
const image = `${repositoryUrl}:${tag}`

if (imageExists(repositoryName, tag)) {
  console.log(`La imagen ${image} ya existe en ECR (el repositorio es inmutable): no se vuelve a subir.`)
} else {
  const password = run('aws', ['ecr', 'get-login-password', '--region', REGION])
  run('docker', ['login', '--username', 'AWS', '--password-stdin', registry], { input: password })
  show('docker', ['build', '--platform', 'linux/amd64', '-t', image, '.'], { cwd: BACKEND_DIR })
  show('docker', ['push', image])
}

console.log(`\nListo. Pon esto en terraform/envs/qa/terraform.tfvars y haz terraform apply:\n`)
console.log(`api_image_tag     = "${tag}"`)
console.log('api_desired_count = 1')
