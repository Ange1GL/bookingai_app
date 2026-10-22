#!/usr/bin/env node
// Spike de Web Push (fase 0). Servidor mínimo: sirve public/, guarda suscripciones y envía notificaciones de prueba.
// No es código de producción; solo confirma que el flujo funciona en los dispositivos reales.
import { createServer } from 'http'
import { existsSync, readFileSync, writeFileSync } from 'fs'
import { dirname, extname, join, resolve } from 'path'
import { fileURLToPath } from 'url'
import webpush from 'web-push'

const __dirname = dirname(fileURLToPath(import.meta.url))
const PUBLIC_DIR = join(__dirname, 'public')
const VAPID_FILE = join(__dirname, '.vapid.json')
const SUBSCRIPTIONS_FILE = join(__dirname, 'subscriptions.json')
const PORT = Number(process.env.PORT ?? 4300)
const VAPID_SUBJECT = process.env.VAPID_SUBJECT ?? 'mailto:spike@example.com'
const DEFAULT_DELAY_SECONDS = 5
const MAX_DELAY_SECONDS = 120
const PUSH_TTL_SECONDS = 300

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
  '.png': 'image/png',
}

function loadVapidKeys() {
  if (existsSync(VAPID_FILE)) {
    return JSON.parse(readFileSync(VAPID_FILE, 'utf8'))
  }
  const keys = webpush.generateVAPIDKeys()
  writeFileSync(VAPID_FILE, JSON.stringify(keys, null, 2))
  console.log('Claves VAPID nuevas guardadas en .vapid.json (ignorado por git)')
  return keys
}

function loadSubscriptions() {
  return existsSync(SUBSCRIPTIONS_FILE) ? JSON.parse(readFileSync(SUBSCRIPTIONS_FILE, 'utf8')) : []
}

function saveSubscriptions(subscriptions) {
  writeFileSync(SUBSCRIPTIONS_FILE, JSON.stringify(subscriptions, null, 2))
}

async function readJson(request) {
  const chunks = []
  for await (const chunk of request) {
    chunks.push(chunk)
  }
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {}
}

function sendJson(response, status, body) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify(body))
}

function serveStatic(urlPath, response) {
  const relative = urlPath === '/' ? 'index.html' : urlPath.slice(1)
  const file = resolve(PUBLIC_DIR, relative)
  if (!file.startsWith(PUBLIC_DIR) || !existsSync(file)) {
    response.writeHead(404)
    response.end('Not found')
    return
  }
  // El service worker nunca debe quedarse en caché: igual que en producción (CloudFront).
  response.writeHead(200, {
    'Content-Type': MIME_TYPES[extname(file)] ?? 'application/octet-stream',
    'Cache-Control': 'no-cache',
  })
  response.end(readFileSync(file))
}

async function pushTo(subscription, payload, vapid) {
  try {
    await webpush.sendNotification(subscription, JSON.stringify(payload), {
      TTL: PUSH_TTL_SECONDS,
      urgency: 'high',
      vapidDetails: { subject: VAPID_SUBJECT, publicKey: vapid.publicKey, privateKey: vapid.privateKey },
    })
    return { endpoint: shortEndpoint(subscription.endpoint), result: 'sent' }
  } catch (error) {
    const gone = error.statusCode === 404 || error.statusCode === 410
    return { endpoint: shortEndpoint(subscription.endpoint), result: gone ? 'gone' : 'failed', status: error.statusCode, gone }
  }
}

function shortEndpoint(endpoint) {
  return new URL(endpoint).host
}

const vapid = loadVapidKeys()

const server = createServer(async (request, response) => {
  const { pathname } = new URL(request.url, `http://${request.headers.host}`)
  try {
    if (request.method === 'GET' && pathname === '/api/public-key') {
      return sendJson(response, 200, { publicKey: vapid.publicKey })
    }
    if (request.method === 'POST' && pathname === '/api/subscribe') {
      const subscription = await readJson(request)
      const others = loadSubscriptions().filter((item) => item.endpoint !== subscription.endpoint)
      saveSubscriptions([...others, subscription])
      return sendJson(response, 201, { stored: others.length + 1 })
    }
    if (request.method === 'POST' && pathname === '/api/send') {
      const { delaySeconds = DEFAULT_DELAY_SECONDS } = await readJson(request)
      const delay = Math.min(Math.max(Number(delaySeconds) || 0, 0), MAX_DELAY_SECONDS)
      const subscriptions = loadSubscriptions()
      sendJson(response, 202, { scheduledInSeconds: delay, devices: subscriptions.length })
      // El retraso permite bloquear el teléfono o cerrar la app antes de que llegue el aviso.
      setTimeout(async () => {
        const payload = { title: 'En 5 min: Juan Pérez', body: 'Corte y barba · 4:30 p. m.', url: '/?from=push', tag: 'reminder-spike' }
        const results = await Promise.all(subscriptions.map((item) => pushTo(item, payload, vapid)))
        saveSubscriptions(subscriptions.filter((_, index) => !results[index].gone))
        console.log('Push enviada:', JSON.stringify(results))
      }, delay * 1000)
      return
    }
    return serveStatic(pathname, response)
  } catch (error) {
    console.error(error)
    sendJson(response, 500, { error: 'internal' })
  }
})

server.listen(PORT, () => console.log(`Spike de push en http://localhost:${PORT}`))
