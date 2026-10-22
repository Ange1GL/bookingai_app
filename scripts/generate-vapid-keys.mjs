#!/usr/bin/env node
// Genera un par de claves VAPID (ECDSA P-256, base64url) para los recordatorios push.
// Uso: node scripts/generate-vapid-keys.mjs
// Pega la salida en backend/.env (dev) o en Parameter Store (qa). NUNCA las subas al repositorio.
import { createECDH } from 'crypto'

const CURVE = 'prime256v1'

function toBase64Url(buffer) {
  return buffer.toString('base64url')
}

const ecdh = createECDH(CURVE)
ecdh.generateKeys()

console.log('# Claves VAPID — guárdalas como secretos; la privada no se comparte ni se commitea.')
console.log(`VAPID_PUBLIC_KEY=${toBase64Url(ecdh.getPublicKey())}`)
console.log(`VAPID_PRIVATE_KEY=${toBase64Url(ecdh.getPrivateKey())}`)
console.log('VAPID_SUBJECT=mailto:tu-correo@ejemplo.com')
