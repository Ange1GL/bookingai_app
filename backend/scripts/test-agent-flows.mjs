#!/usr/bin/env node
// scripts/test-agent-flows.mjs
// Ejecuta los 7 flujos conversacionales contra el agente de barbería y genera un reporte.
// Uso: node scripts/test-agent-flows.mjs
// Requiere: Node 18+ (fetch nativo), app corriendo en http://localhost:8082

import { writeFileSync } from 'fs'
import { dirname, resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const BASE = 'http://localhost:8082/api/v1'

// Delay entre requests para no saturar el rate limit del proveedor de IA (DeepSeek).
// Ajustar si se siguen recibiendo 429.
const DELAY_MS = 1500

const sleep = (ms) => new Promise((res) => setTimeout(res, ms))

async function post(path, body = undefined) {
  await sleep(DELAY_MS)
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`POST ${path} → HTTP ${res.status}: ${text}`)
  }
  return res.json()
}

async function createSession() {
  const data = await post('/agent/session')
  return data.sessionId
}

async function chat(sessionId, message) {
  const data = await post('/agent/chat', { sessionId, message })
  return data.reply
}

// ---------------------------------------------------------------------------
// Seed — clientes
// ---------------------------------------------------------------------------

async function seedCustomers() {
  console.log('\n🌱 Sembrando clientes...')
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
    const created = await post('/customers', c)
    ids[c.phone] = created.id
    console.log(`  ✓ ${c.name} (${c.phone}) → id ${created.id}`)
  }
  return ids
}

// ---------------------------------------------------------------------------
// Seed — citas previas
// ---------------------------------------------------------------------------

async function seedAppointments(customerIds) {
  console.log('\n🌱 Sembrando citas previas...')
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
    const created = await post('/appointments', body)
    console.log(`  ✓ ${label} → id ${created.id}`)
  }
}

// ---------------------------------------------------------------------------
// Ejecución de flujo
// ---------------------------------------------------------------------------

async function runFlow(flow) {
  console.log(`\n▶  ${flow.name}`)
  const sessionId = await createSession()
  const log = []
  let lastReply = ''

  for (const { message } of flow.turns) {
    console.log(`   💬 Barbero: "${message}"`)
    const reply = await chat(sessionId, message)
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

// ---------------------------------------------------------------------------
// Definición de los 7 flujos
// ---------------------------------------------------------------------------

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
// Generación del reporte
// ---------------------------------------------------------------------------

function generateReport(results) {
  const now = new Date().toISOString()
  const pass = results.filter((r) => r.status === '✅ PASS').length
  const fail = results.filter((r) => r.status === '❌ FAIL').length

  const summaryRows = results
    .map((r) => `| ${r.name} | ${r.status} | \`${r.keywords.join(', ')}\` |`)
    .join('\n')

  const detailSections = results
    .map((r) => {
      const turns = r.log
        .map(
          (t, i) =>
            `**Turno ${i * 2 + 1} — Barbero:** ${t.barbero}\n\n**Turno ${i * 2 + 2} — IA:** ${t.ia}`
        )
        .join('\n\n---\n\n')

      return [
        `### ${r.name}`,
        '',
        turns,
        '',
        `**Estado:** ${r.status}`,
        `**Keywords buscadas:** \`${r.keywords.join(', ')}\``,
        `**Última respuesta IA:**`,
        '```',
        r.lastReply,
        '```',
      ].join('\n')
    })
    .join('\n\n---\n\n')

  const report = `# Reporte de Pruebas — Agente Barbería

**Fecha:** ${now}
**Entorno:** ${BASE}
**Modelo:** deepseek-flash
**Delay entre requests:** ${DELAY_MS}ms

---

## Resumen

| Métrica | Valor |
|---|---|
| ✅ PASS | ${pass} |
| ❌ FAIL | ${fail} |
| Total | ${results.length} |

| Flujo | Estado | Keywords buscadas |
|---|---|---|
${summaryRows}

---

## Detalle por flujo

${detailSections}
`

  const outPath = resolve(__dirname, '../docs/reporte-pruebas.md')
  writeFileSync(outPath, report, 'utf8')
  return outPath
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  console.log('🚀 Pruebas del agente de barbería')
  console.log(`   Endpoint : ${BASE}`)
  console.log(`   Delay    : ${DELAY_MS}ms entre requests`)

  const customerIds = await seedCustomers()
  await seedAppointments(customerIds)

  const flows = buildFlows()
  const results = []
  for (const flow of flows) {
    const result = await runFlow(flow)
    results.push(result)
  }

  const outPath = generateReport(results)
  console.log(`\n📄 Reporte generado: ${outPath}`)

  const failed = results.filter((r) => r.status === '❌ FAIL')
  if (failed.length > 0) {
    console.log(`\n⚠️  ${failed.length} flujo(s) fallaron:`)
    failed.forEach((r) => console.log(`   - ${r.name}`))
    console.log('   Revisa docs/reporte-pruebas.md para ver las respuestas completas.')
    process.exit(1)
  }

  console.log('\n✅ Todos los flujos pasaron.')
}

main().catch((err) => {
  console.error('\n💥 Error fatal:', err.message)
  process.exit(1)
})
