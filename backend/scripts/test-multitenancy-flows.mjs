#!/usr/bin/env node
// scripts/test-multitenancy-flows.mjs
// Verifica dos cosas contra la app corriendo en http://localhost:8082:
//   1) Regresión: los 7 flujos conversacionales originales (test-agent-flows.mjs) siguen
//      funcionando ahora que todo el API requiere JWT, autenticado como el dueño real
//      de los datos sembrados (lariosacostaa@gmail.com).
//   2) Aislamiento multi-tenant: un segundo usuario (miguel@correo.com) NO puede ver ni
//      operar sobre los clientes del primero, tras el refactor de Customer.userId.
// Uso: node scripts/test-multitenancy-flows.mjs
// Requiere: Node 18+ (fetch nativo), app corriendo en http://localhost:8082.
// Credenciales: ver docs/credenciales_app.md — ambos usuarios deben existir ya en la BD.

import { writeFileSync } from 'fs'
import { dirname, resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const BASE = 'http://localhost:8082/api/v1'

const OWNER = { email: 'lariosacostaa@gmail.com', password: 'Glpi12345$' }
const OTHER = { email: 'miguel@correo.com', password: 'Glpi12345$' }

// Delay entre requests para no saturar el rate limit del proveedor de IA (DeepSeek).
const DELAY_MS = 1500

const sleep = (ms) => new Promise((res) => setTimeout(res, ms))

// ---------------------------------------------------------------------------
// HTTP helpers
// ---------------------------------------------------------------------------

async function rawPost(path, body, token) {
  await sleep(DELAY_MS)
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  return res
}

// Lanza si la respuesta no es ok — para el camino feliz.
async function post(path, body, token) {
  const res = await rawPost(path, body, token)
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`POST ${path} → HTTP ${res.status}: ${text}`)
  }
  return res.json()
}

// No lanza — para los tests negativos donde se espera un 401/403/404.
async function postExpectingStatus(path, body, token) {
  const res = await rawPost(path, body, token)
  let data = null
  try {
    data = await res.json()
  } catch {
    // body vacío o no-JSON, se ignora
  }
  return { status: res.status, ok: res.ok, data }
}

async function login({ email, password }) {
  const res = await rawPost('/auth/login', { email, password })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(
      `Login falló para ${email} (HTTP ${res.status}): ${text}\n` +
      `¿Existe ese usuario en la BD? Créalo con POST ${BASE}/auth/register ` +
      `{ "email": "${email}", "password": "${password}", "name": "..." } y vuelve a correr el script.`
    )
  }
  const data = await res.json()
  return data.token
}

async function createSession(token) {
  const data = await post('/agent/session', undefined, token)
  return data.sessionId
}

async function chat(token, sessionId, message) {
  const data = await post('/agent/chat', { sessionId, message }, token)
  return data.reply
}

// ---------------------------------------------------------------------------
// Seed — clientes y citas (como OWNER)
// ---------------------------------------------------------------------------

async function seedCustomers(token) {
  console.log('\n🌱 Sembrando clientes (OWNER)...')
  const customers = [
    { name: 'Sonia Acosta',             phone: '527714056025' },
    { name: 'Luis de la Cruz Roque',    phone: '527298339824' },
    { name: 'Luis de la Cruz Roque',    phone: '527298339825' }, // duplicado (último dígito +1)
    { name: 'Carlos Cruz',              phone: '527713905509' },
    { name: 'Carlos Javier',            phone: '527711240938' },
    // Marisol Acosta NO se siembra aquí — se registra en el Flujo 2
  ]

  const ids = {}
  for (const c of customers) {
    const created = await post('/customers', c, token)
    ids[c.phone] = created.id
    console.log(`  ✓ ${c.name} (${c.phone}) → id ${created.id}`)
  }
  return ids
}

async function seedAppointments(token, customerIds) {
  console.log('\n🌱 Sembrando citas previas (OWNER)...')
  const appointments = [
    {
      label: 'Carlos Cruz — Sep 14 17:00 (Flujo 5)',
      customerId: customerIds['527713905509'],
      startTime: '2026-09-14T17:00:00',
      endTime:   '2026-09-14T17:30:00',
    },
    {
      label: 'Carlos Cruz — Sep 16 11:00 (Flujo 5)',
      customerId: customerIds['527713905509'],
      startTime: '2026-09-16T11:00:00',
      endTime:   '2026-09-16T11:30:00',
    },
    {
      label: 'Luis ...9824 — Sep 14 18:00 (Flujo 6)',
      customerId: customerIds['527298339824'],
      startTime: '2026-09-14T18:00:00',
      endTime:   '2026-09-14T18:30:00',
    },
  ]

  for (const { label, ...body } of appointments) {
    const created = await post('/appointments', body, token)
    console.log(`  ✓ ${label} → id ${created.id}`)
  }
}

// ---------------------------------------------------------------------------
// Fase 1 — Regresión: los 7 flujos conversacionales originales, autenticados
// ---------------------------------------------------------------------------

async function runFlow(token, flow) {
  console.log(`\n▶  ${flow.name}`)
  const sessionId = await createSession(token)
  const log = []
  let lastReply = ''

  for (const { message } of flow.turns) {
    console.log(`   💬 Barbero: "${message}"`)
    const reply = await chat(token, sessionId, message)
    lastReply = reply
    console.log(`   🤖 IA: "${reply}"`)
    log.push({ barbero: message, ia: reply })
  }

  const pass = flow.keywords.some((kw) =>
    lastReply.toLowerCase().includes(kw.toLowerCase())
  )
  const status = pass ? '✅ PASS' : '❌ FAIL'
  console.log(`   ${status} (keywords: ${flow.keywords.join(' | ')})`)

  return { name: flow.name, status, log, keywords: flow.keywords, lastReply }
}

function buildFlows() {
  return [
    {
      name: 'Flujo 1 — Crear cita: Sonia Acosta (cliente existente)',
      keywords: ['sonia', 'guardada', 'agendada', '3:30', 'cita'],
      turns: [
        { message: 'Agenda una cita para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m.' },
        { message: 'Sí, es para ella.' },
      ],
    },
    {
      name: 'Flujo 2 — Crear cita: Marisol Acosta (cliente nueva)',
      keywords: ['marisol', 'registrada', 'guardada', 'agendada', '4:45'],
      turns: [
        { message: 'Agenda una cita para Marisol Acosta el 13 de septiembre de 2026 a las 4:00 p.m.' },
        { message: 'Su número es 771 720 3342.' },
        { message: 'Sí, correcto.' },
        { message: 'Corte y barba, 45 minutos.' },
      ],
    },
    {
      name: 'Flujo 3 — Crear cita: ambigüedad "Carlos"',
      keywords: ['carlos cruz', 'agendada', 'guardada', '6:00', '15 de septiembre'],
      turns: [
        { message: 'Agenda una cita para Carlos el 15 de septiembre de 2026 a las 6:00 p.m.' },
        { message: 'Es el que termina en 9509.' },
      ],
    },
    {
      name: 'Flujo 4 — Cancelar cita: por horario (Sonia Acosta Sep 11)',
      keywords: ['cancelad', 'libre', 'sonia'],
      turns: [
        { message: 'Cancela la cita de las 3:00 p.m. del 11 de septiembre de 2026.' },
        { message: 'Sí, confírmala.' },
      ],
    },
    {
      name: 'Flujo 5 — Cancelar cita: Carlos Cruz (2 citas activas)',
      keywords: ['cancelad', '14 de septiembre', 'carlos'],
      turns: [
        { message: 'Cancela la cita de Carlos Cruz.' },
        { message: 'La del 14.' },
        { message: 'Sí.' },
      ],
    },
    {
      name: 'Flujo 6 — Reagendar: Luis de la Cruz Roque sin conflicto',
      keywords: ['reubicad', 'movid', 'actualiz', '18 de septiembre'],
      turns: [
        { message: 'Mueve la cita de Luis de la Cruz Roque del 14 de septiembre de 2026 a las 6:00 p.m. para el 18 de septiembre de 2026 a la misma hora.' },
        { message: 'Sí, muévela.' },
      ],
    },
    {
      name: 'Flujo 7 — Reagendar: Marisol Acosta con conflicto en Sep 18 18:00',
      keywords: ['6:30', 'actualiz', 'movid', 'reubicad', 'marisol'],
      turns: [
        { message: 'Cambia la cita de Marisol Acosta para el 18 de septiembre de 2026 a las 6:00 p.m.' },
        { message: 'A las 6:30 p.m.' },
      ],
    },
  ]
}

// ---------------------------------------------------------------------------
// Fase 2 — Aislamiento multi-tenant (como OTHER, miguel@correo.com)
// ---------------------------------------------------------------------------

async function runIsolationTests(otherToken, ownerCustomerIds) {
  console.log('\n🔒 Pruebas de aislamiento multi-tenant (OTHER = miguel@correo.com)')
  const results = []

  // Test A — búsqueda cruzada por IA: miguel no debe encontrar a Sonia (de OWNER)
  {
    console.log('\n▶  Test A — Búsqueda cruzada por IA (Sonia Acosta es de OWNER)')
    const sessionId = await createSession(otherToken)
    const message = 'Busca al cliente Sonia Acosta'
    console.log(`   💬 Barbero (miguel): "${message}"`)
    const reply = await chat(otherToken, sessionId, message)
    console.log(`   🤖 IA: "${reply}"`)
    const notFoundKeywords = ['no encontr', 'no existe', 'no tengo registro', 'no aparece', 'no hay coincidencia', 'ningún cliente', 'ninguna cliente']
    const pass = notFoundKeywords.some((kw) => reply.toLowerCase().includes(kw))
    const status = pass ? '✅ PASS' : '❌ FAIL'
    console.log(`   ${status} (se esperaba que NO encontrara a Sonia Acosta)`)
    results.push({
      name: 'Test A — Búsqueda cruzada por IA no debe encontrar clientes ajenos',
      status,
      log: [{ barbero: message, ia: reply }],
      keywords: notFoundKeywords,
      lastReply: reply,
    })
  }

  // Test B — crear cita sobre un customerId de OWNER debe fallar con 404
  {
    console.log('\n▶  Test B — Crear cita con customerId ajeno (se espera 404)')
    const soniaId = ownerCustomerIds['527714056025']
    const body = { customerId: soniaId, startTime: '2026-09-20T10:00:00', endTime: '2026-09-20T10:30:00' }
    const { status: httpStatus, data } = await postExpectingStatus('/appointments', body, otherToken)
    console.log(`   → HTTP ${httpStatus} ${data ? JSON.stringify(data) : ''}`)
    const pass = httpStatus === 404
    const status = pass ? '✅ PASS' : '❌ FAIL'
    console.log(`   ${status} (se esperaba HTTP 404, customerId ${soniaId} pertenece a OWNER)`)
    results.push({
      name: 'Test B — Crear cita con customerId de otro tenant debe responder 404',
      status,
      log: [{ barbero: `POST /appointments customerId=${soniaId}`, ia: `HTTP ${httpStatus}` }],
      keywords: ['404'],
      lastReply: `HTTP ${httpStatus}`,
    })
  }

  // Test C — mismo teléfono, tenant distinto → debe crear un Customer nuevo, no reusar el de OWNER
  {
    console.log('\n▶  Test C — Mismo teléfono que Sonia Acosta, tenant distinto')
    const soniaOwnerId = ownerCustomerIds['527714056025']
    const created = await post('/customers', { name: 'Sonia (cliente de miguel)', phone: '527714056025' }, otherToken)
    console.log(`   → Customer creado por miguel: id ${created.id} (Sonia de OWNER es id ${soniaOwnerId})`)
    const pass = created.id !== soniaOwnerId
    const status = pass ? '✅ PASS' : '❌ FAIL'
    console.log(`   ${status} (deben ser IDs distintos — find-or-create scoped por userId)`)
    results.push({
      name: 'Test C — Mismo teléfono en tenant distinto crea un Customer separado',
      status,
      log: [{ barbero: 'POST /customers phone=527714056025 (como miguel)', ia: `id ${created.id} (OWNER: ${soniaOwnerId})` }],
      keywords: ['id distinto'],
      lastReply: `id ${created.id} vs ${soniaOwnerId}`,
    })
  }

  // Test D — control positivo: el flujo normal de miguel no debe romperse
  {
    console.log('\n▶  Test D — Control positivo: miguel crea su propio cliente y cita')
    const created = await post('/customers', { name: 'Cliente de Prueba Miguel', phone: '527700000001' }, otherToken)
    const appointment = await post('/appointments', {
      customerId: created.id,
      startTime: '2026-09-21T10:00:00',
      endTime: '2026-09-21T10:30:00',
    }, otherToken)
    console.log(`   → Customer id ${created.id}, Appointment id ${appointment.id}`)
    const pass = Boolean(created.id) && Boolean(appointment.id)
    const status = pass ? '✅ PASS' : '❌ FAIL'
    console.log(`   ${status} (el flujo propio de miguel debe funcionar normalmente)`)
    results.push({
      name: 'Test D — Control positivo: flujo propio de miguel funciona',
      status,
      log: [{ barbero: 'POST /customers + POST /appointments (como miguel)', ia: `Customer ${created.id}, Appointment ${appointment.id}` }],
      keywords: ['ids generados'],
      lastReply: `Customer ${created.id}, Appointment ${appointment.id}`,
    })
  }

  return results
}

// ---------------------------------------------------------------------------
// Reporte
// ---------------------------------------------------------------------------

function generateReport(regressionResults, isolationResults) {
  const now = new Date().toISOString()
  const all = [...regressionResults, ...isolationResults]
  const pass = all.filter((r) => r.status === '✅ PASS').length
  const fail = all.filter((r) => r.status === '❌ FAIL').length

  const summaryRows = all
    .map((r) => `| ${r.name} | ${r.status} | \`${r.keywords.join(', ')}\` |`)
    .join('\n')

  const detailSections = all
    .map((r) => {
      const turns = r.log
        .map(
          (t, i) =>
            `**Turno ${i * 2 + 1} — Barbero:** ${t.barbero}\n\n**Turno ${i * 2 + 2} — IA/Resultado:** ${t.ia}`
        )
        .join('\n\n---\n\n')

      return [
        `### ${r.name}`,
        '',
        turns,
        '',
        `**Estado:** ${r.status}`,
        `**Keywords/condición buscada:** \`${r.keywords.join(', ')}\``,
        `**Última respuesta:**`,
        '```',
        r.lastReply,
        '```',
      ].join('\n')
    })
    .join('\n\n---\n\n')

  const report = `# Reporte de Pruebas — Multi-tenancy de Customer

**Fecha:** ${now}
**Entorno:** ${BASE}
**Usuario OWNER:** ${OWNER.email}
**Usuario OTHER:** ${OTHER.email}
**Delay entre requests:** ${DELAY_MS}ms

---

## Resumen

| Métrica | Valor |
|---|---|
| ✅ PASS | ${pass} |
| ❌ FAIL | ${fail} |
| Total | ${all.length} |

| Flujo / Test | Estado | Keywords / condición |
|---|---|---|
${summaryRows}

---

## Detalle

${detailSections}
`

  const outPath = resolve(__dirname, '../docs/reporte-pruebas-multitenancy.md')
  writeFileSync(outPath, report, 'utf8')
  return outPath
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  console.log('🚀 Pruebas de multi-tenancy — Customer.userId')
  console.log(`   Endpoint : ${BASE}`)
  console.log(`   Delay    : ${DELAY_MS}ms entre requests`)

  console.log(`\n🔑 Login como OWNER (${OWNER.email})...`)
  const ownerToken = await login(OWNER)

  console.log('\n--- Fase 1: regresión de los 7 flujos originales (autenticado) ---')
  const customerIds = await seedCustomers(ownerToken)
  await seedAppointments(ownerToken, customerIds)

  const flows = buildFlows()
  const regressionResults = []
  for (const flow of flows) {
    const result = await runFlow(ownerToken, flow)
    regressionResults.push(result)
  }

  console.log(`\n🔑 Login como OTHER (${OTHER.email})...`)
  const otherToken = await login(OTHER)

  console.log('\n--- Fase 2: aislamiento multi-tenant ---')
  const isolationResults = await runIsolationTests(otherToken, customerIds)

  const outPath = generateReport(regressionResults, isolationResults)
  console.log(`\n📄 Reporte generado: ${outPath}`)

  const failed = [...regressionResults, ...isolationResults].filter((r) => r.status === '❌ FAIL')
  if (failed.length > 0) {
    console.log(`\n⚠️  ${failed.length} prueba(s) fallaron:`)
    failed.forEach((r) => console.log(`   - ${r.name}`))
    console.log('   Revisa docs/reporte-pruebas-multitenancy.md para ver el detalle completo.')
    process.exit(1)
  }

  console.log('\n✅ Todas las pruebas pasaron (regresión + aislamiento multi-tenant).')
}

main().catch((err) => {
  console.error('\n💥 Error fatal:', err.message)
  process.exit(1)
})
