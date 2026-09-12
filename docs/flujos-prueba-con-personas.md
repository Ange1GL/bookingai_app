# Flujos de Prueba — Agente Barbería (Datos Reales)

Endpoint base: `http://localhost:8082/api/v1`
Modelo IA: `deepseek-flash`
Fechas: septiembre 2026

---

## Estado inicial de la base de datos

Ejecutar el seed antes de cualquier flujo. El script `scripts/test-agent-flows.mjs` lo hace automáticamente.

### Clientes a sembrar

| Nombre | Teléfono (E.164 sin +) | Rol en flujos |
|---|---|---|
| Sonia Acosta | `527714056025` | Flujo 1, 4 |
| Luis de la Cruz Roque | `527298339824` | Flujo 6 (reagenda) |
| Luis de la Cruz Roque *(dup)* | `527298339825` | Flujo 3 (no usado), contexto ambigüedad |
| Carlos Cruz | `527713905509` | Flujo 3, 5 |
| Carlos Javier | `527711240938` | Flujo 3 (ambigüedad) |
| **Marisol Acosta** | `527717203342` | Flujo 2 (se registra aquí), Flujo 7 |

> Marisol Acosta **no se siembra**. Se crea durante el Flujo 2.

### Citas a sembrar

| Cliente | Fecha/hora inicio | Fecha/hora fin | Usado en |
|---|---|---|---|
| Carlos Cruz (`...9509`) | 2026-09-14T17:00 | 2026-09-14T17:30 | Flujo 5 |
| Carlos Cruz (`...9509`) | 2026-09-16T11:00 | 2026-09-16T11:30 | Flujo 5 |
| Luis de la Cruz Roque (`...9824`) | 2026-09-14T18:00 | 2026-09-14T18:30 | Flujo 6 |

---

## Dependencias entre flujos

```
Flujo 1 ──► Flujo 4   (Flujo 4 cancela la cita creada en Flujo 1)
Flujo 2 ──► Flujo 7   (Flujo 7 reagenda la cita de Marisol creada en Flujo 2)
Flujo 6 ──► Flujo 7   (Flujo 7 choca con Luis que quedó en Sep 18 tras Flujo 6)
```

Ejecutar siempre en orden: 1 → 2 → 3 → 4 → 5 → 6 → 7.

---

## Flujo 1 — Crear cita: cliente existente (coincidencia única)

**Pre-condición:** Sonia Acosta (`527714056025`) existe en la BD.
**Sesión:** nueva (`POST /api/v1/agent/session`)

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Agenda una cita para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m. |
| 2 | IA *(esperado)* | Encontré a Sonia Acosta con el teléfono ...6025. ¿Es para ella la cita o se trata de un cliente nuevo? |
| 3 | Barbero | Sí, es para ella. |
| 4 | IA *(esperado)* | Perfecto. Cita guardada para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m. |

**Resultado esperado en BD:** nueva cita `PENDING` para Sonia Acosta, `startTime = 2026-09-11T15:00`, `endTime = 2026-09-11T15:30`.
**Keywords a verificar en última respuesta IA:** `sonia`, `guardada`, `agendada`, `3:30`

---

## Flujo 2 — Crear cita: cliente nuevo (sin coincidencias)

**Pre-condición:** Marisol Acosta no existe en la BD.
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Agenda una cita para Marisol Acosta el 13 de septiembre de 2026 a las 4:00 p.m. |
| 2 | IA *(esperado)* | No encontré a ninguna Marisol Acosta. Para registrarla, ¿cuál es su número telefónico? |
| 3 | Barbero | Su número es 771 720 3342. |
| 4 | IA *(esperado)* | Confirmando número: 771 720 3342. ¿Es correcto? |
| 5 | Barbero | Sí, correcto. |
| 6 | IA *(esperado)* | Cliente registrada. ¿Qué servicio se realizará y cuántos minutos durará? |
| 7 | Barbero | Corte y barba, 45 minutos. |
| 8 | IA *(esperado)* | Cita guardada para Marisol Acosta el 13 de septiembre de 2026 de 4:00 p.m. a 4:45 p.m. |

**Resultado esperado en BD:** nuevo cliente Marisol Acosta `phone=527717203342` + cita `PENDING` `2026-09-13T16:00–16:45`.
**Keywords a verificar en última respuesta IA:** `marisol`, `guardada`, `agendada`, `4:45`, `registrada`

---

## Flujo 3 — Crear cita: ambigüedad (múltiples coincidencias "Carlos")

**Pre-condición:** Carlos Cruz (`...9509`) y Carlos Javier (`...0938`) existen en la BD.
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Agenda una cita para Carlos el 15 de septiembre de 2026 a las 6:00 p.m. |
| 2 | IA *(esperado)* | Tengo 2 clientes con el nombre "Carlos": Carlos Cruz (Tel: ...9509) y Carlos Javier (Tel: ...0938). ¿Se trata de alguno de ellos o es un cliente nuevo? |
| 3 | Barbero | Es el que termina en 9509. |
| 4 | IA *(esperado)* | Cita agendada para Carlos Cruz (...9509) el 15 de septiembre de 2026 a las 6:00 p.m. |

**Resultado esperado en BD:** nueva cita `PENDING` para Carlos Cruz, `2026-09-15T18:00–18:30`.
**Keywords a verificar en última respuesta IA:** `carlos cruz`, `agendada`, `guardada`, `6:00`, `15 de septiembre`

---

## Flujo 4 — Cancelar cita: por horario

**Pre-condición:** Existe la cita de Sonia Acosta del Flujo 1 (Sep 11, 15:00–15:30).
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Cancela la cita de las 3:00 p.m. del 11 de septiembre de 2026. |
| 2 | IA *(esperado)* | La cita de las 3:00 p.m. pertenece a Sonia Acosta. ¿Confirmas que deseas cancelar esta cita? |
| 3 | Barbero | Sí, confírmala. |
| 4 | IA *(esperado)* | Cita cancelada correctamente. El espacio de 3:00 p.m. a 3:30 p.m. del 11 de septiembre de 2026 ha quedado libre. |

**Resultado esperado en BD:** cita de Sonia Sep 11 pasa a `CANCELLED` (`statusId = 2`).
**Keywords a verificar en última respuesta IA:** `cancelad`, `libre`, `sonia`

---

## Flujo 5 — Cancelar cita: nombre ambiguo (cliente con varias citas)

**Pre-condición:** Carlos Cruz tiene 2 citas activas sembradas: Sep 14 17:00 y Sep 16 11:00.
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Cancela la cita de Carlos Cruz. |
| 2 | IA *(esperado)* | Carlos Cruz tiene dos citas agendadas: 1) 14 de septiembre de 2026 a las 5:00 p.m. 2) 16 de septiembre de 2026 a las 11:00 a.m. ¿Cuál deseas cancelar? |
| 3 | Barbero | La del 14. |
| 4 | IA *(esperado)* | ¿Confirmas la cancelación de la cita de Carlos Cruz para el 14 de septiembre de 2026 a las 5:00 p.m.? |
| 5 | Barbero | Sí. |
| 6 | IA *(esperado)* | Cita del 14 de septiembre de 2026 a las 5:00 p.m. cancelada. |

**Resultado esperado en BD:** cita de Carlos Cruz Sep 14 pasa a `CANCELLED`. La del Sep 16 permanece `PENDING`.
**Keywords a verificar en última respuesta IA:** `cancelad`, `14 de septiembre`, `carlos`

---

## Flujo 6 — Reagendar: sin conflicto

**Pre-condición:** Luis de la Cruz Roque (`...9824`) tiene cita Sep 14 18:00–18:30. Sep 18 18:00 está libre.
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Mueve la cita de Luis de la Cruz Roque del 14 de septiembre de 2026 a las 6:00 p.m. para el 18 de septiembre de 2026 a la misma hora. |
| 2 | IA *(esperado)* | Verificando disponibilidad para el 18 de septiembre de 2026 a las 6:00 p.m... El horario está libre. ¿Confirmas mover la cita de Luis de la Cruz Roque? |
| 3 | Barbero | Sí, muévela. |
| 4 | IA *(esperado)* | Cita reubicada exitosamente para Luis de la Cruz Roque el 18 de septiembre de 2026 de 6:00 p.m. a 6:30 p.m. |

**Resultado esperado en BD:** cita de Luis `startTime` cambia a `2026-09-18T18:00`, `endTime` a `2026-09-18T18:30`.
**Keywords a verificar en última respuesta IA:** `reubicad`, `movid`, `actualiz`, `18 de septiembre`

---

## Flujo 7 — Reagendar: con conflicto de agenda

**Pre-condición:** Luis de la Cruz Roque está en Sep 18 18:00–18:30 (resultado del Flujo 6). Marisol Acosta tiene su cita de Sep 13 (creada en Flujo 2) activa.
**Sesión:** nueva

| Turno | Rol | Mensaje |
|---|---|---|
| 1 | Barbero | Cambia la cita de Marisol Acosta para el 18 de septiembre de 2026 a las 6:00 p.m. |
| 2 | IA *(esperado)* | El horario del 18 de septiembre de 2026 a las 6:00 p.m. ya está ocupado por Luis de la Cruz Roque. Tienes disponible a las 6:30 p.m. o a las 7:00 p.m. ¿A qué hora prefieres moverla? |
| 3 | Barbero | A las 6:30 p.m. |
| 4 | IA *(esperado)* | Cita de Marisol Acosta actualizada para el 18 de septiembre de 2026 a las 6:30 p.m. |

**Resultado esperado en BD:** cita de Marisol `startTime = 2026-09-18T18:30`, `endTime = 2026-09-18T19:15` (45 min de corte y barba).
**Keywords a verificar en última respuesta IA:** `6:30`, `actualiz`, `movid`, `reubicad`, `marisol`

---

## Checklist de verificación manual

- [ ] Flujo 1: cita Sonia Sep 11 15:00 existe con `status = PENDING`
- [ ] Flujo 2: Marisol Acosta creada + cita Sep 13 16:00 con `status = PENDING`
- [ ] Flujo 3: cita Carlos Cruz Sep 15 18:00 con `status = PENDING`
- [ ] Flujo 4: cita Sonia Sep 11 15:00 tiene `status = CANCELLED`
- [ ] Flujo 5: cita Carlos Cruz Sep 14 17:00 tiene `status = CANCELLED`; Sep 16 sigue `PENDING`
- [ ] Flujo 6: cita Luis pasa de Sep 14 a Sep 18 18:00
- [ ] Flujo 7: cita Marisol pasa a Sep 18 18:30
