# Reporte de Pruebas — Multi-tenancy de Customer

**Fecha:** 2026-09-13T16:53:13.576Z
**Entorno:** http://localhost:8082/api/v1
**Usuario OWNER:** lariosacostaa@gmail.com
**Usuario OTHER:** miguel@correo.com
**Delay entre requests:** 1500ms

---

## Resumen

| Métrica | Valor |
|---|---|
| ✅ PASS | 11 |
| ❌ FAIL | 0 |
| Total | 11 |

| Flujo / Test | Estado | Keywords / condición |
|---|---|---|
| Flujo 1 — Crear cita: Sonia Acosta (cliente existente) | ✅ PASS | `sonia, guardada, agendada, 3:30, cita` |
| Flujo 2 — Crear cita: Marisol Acosta (cliente nueva) | ✅ PASS | `marisol, registrada, guardada, agendada, 4:45` |
| Flujo 3 — Crear cita: ambigüedad "Carlos" | ✅ PASS | `carlos cruz, agendada, guardada, 6:00, 15 de septiembre` |
| Flujo 4 — Cancelar cita: por horario (Sonia Acosta Sep 11) | ✅ PASS | `cancelad, libre, sonia` |
| Flujo 5 — Cancelar cita: Carlos Cruz (2 citas activas) | ✅ PASS | `cancelad, 14 de septiembre, carlos` |
| Flujo 6 — Reagendar: Luis de la Cruz Roque sin conflicto | ✅ PASS | `reubicad, movid, actualiz, 18 de septiembre` |
| Flujo 7 — Reagendar: Marisol Acosta con conflicto en Sep 18 18:00 | ✅ PASS | `6:30, actualiz, movid, reubicad, marisol` |
| Test A — Búsqueda cruzada por IA no debe encontrar clientes ajenos | ✅ PASS | `no encontr, no existe, no tengo registro, no aparece, no hay coincidencia, ningún cliente, ninguna cliente` |
| Test B — Crear cita con customerId de otro tenant debe responder 404 | ✅ PASS | `404` |
| Test C — Mismo teléfono en tenant distinto crea un Customer separado | ✅ PASS | `id distinto` |
| Test D — Control positivo: flujo propio de miguel funciona | ✅ PASS | `ids generados` |

---

## Detalle

### Flujo 1 — Crear cita: Sonia Acosta (cliente existente)

**Turno 1 — Barbero:** Agenda una cita para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m.

**Turno 2 — IA/Resultado:** Listo ✅ Cita agendada para **Sonia Acosta** el **11 de septiembre de 2026, de 3:00 p.m. a 3:30 p.m.** (ID: 4).

---

**Turno 3 — Barbero:** Sí, es para ella.

**Turno 4 — IA/Resultado:** Verifiqué y la cita ya está registrada:

- **Sonia Acosta** (ID cliente 252)
- **11/09/2026, 15:00–15:30**
- **ID de cita: 4**

No generé un duplicado. ¿Quieres que la mueva o la cancele?

**Estado:** ✅ PASS
**Keywords/condición buscada:** `sonia, guardada, agendada, 3:30, cita`
**Última respuesta:**
```
Verifiqué y la cita ya está registrada:

- **Sonia Acosta** (ID cliente 252)
- **11/09/2026, 15:00–15:30**
- **ID de cita: 4**

No generé un duplicado. ¿Quieres que la mueva o la cancele?
```

---

### Flujo 2 — Crear cita: Marisol Acosta (cliente nueva)

**Turno 1 — Barbero:** Agenda una cita para Marisol Acosta el 13 de septiembre de 2026 a las 4:00 p.m.

**Turno 2 — IA/Resultado:** Listo ✅

Cita agendada para **Marisol Acosta**:
- **Fecha:** 13 de septiembre de 2026
- **Horario:** 4:00 p.m. – 5:00 p.m.
- **ID de cita:** 5

---

**Turno 3 — Barbero:** Su número es 771 720 3342.

**Turno 4 — IA/Resultado:** Confirmo: el cliente ya existía y la cita ya está registrada correctamente.

- **Cliente:** Marisol Acosta (ID 257) — teléfono 771 720 3342
- **Cita:** ID 5 — 13 de septiembre de 2026, 4:00 p.m. – 5:00 p.m. (pendiente)

No se duplicó el registro. ¿Deseas algo más?

---

**Turno 5 — Barbero:** Sí, correcto.

**Turno 6 — IA/Resultado:** Perfecto. ¿Te ayudo con algo más?

---

**Turno 7 — Barbero:** Corte y barba, 45 minutos.

**Turno 8 — IA/Resultado:** Ajustado ✅

- **Cita ID 5** — Marisol Acosta (ID 257)
- **Servicio:** Corte y barba
- **Nuevo horario:** 13 de septiembre de 2026, 4:00 p.m. – 4:45 p.m. (45 min)

Nota: el sistema no guarda el nombre del servicio, solo el horario, así que quedó registrado como cambio de duración.

**Estado:** ✅ PASS
**Keywords/condición buscada:** `marisol, registrada, guardada, agendada, 4:45`
**Última respuesta:**
```
Ajustado ✅

- **Cita ID 5** — Marisol Acosta (ID 257)
- **Servicio:** Corte y barba
- **Nuevo horario:** 13 de septiembre de 2026, 4:00 p.m. – 4:45 p.m. (45 min)

Nota: el sistema no guarda el nombre del servicio, solo el horario, así que quedó registrado como cambio de duración.
```

---

### Flujo 3 — Crear cita: ambigüedad "Carlos"

**Turno 1 — Barbero:** Agenda una cita para Carlos el 15 de septiembre de 2026 a las 6:00 p.m.

**Turno 2 — IA/Resultado:** Encontré dos clientes con nombre similar:

1. **Carlos Javier** – tel. 527711240938
2. **Carlos Cruz** – tel. 527713905509

¿Cuál de los dos es el cliente correcto?

---

**Turno 3 — Barbero:** Es el que termina en 9509.

**Turno 4 — IA/Resultado:** Cita agendada ✅

- **Cliente:** Carlos Cruz (tel. ...9509)
- **Fecha:** 15 de septiembre de 2026, 6:00–7:00 p.m.
- **Cita #:** 6 (estado: pendiente)

¿Necesitas algo más?

**Estado:** ✅ PASS
**Keywords/condición buscada:** `carlos cruz, agendada, guardada, 6:00, 15 de septiembre`
**Última respuesta:**
```
Cita agendada ✅

- **Cliente:** Carlos Cruz (tel. ...9509)
- **Fecha:** 15 de septiembre de 2026, 6:00–7:00 p.m.
- **Cita #:** 6 (estado: pendiente)

¿Necesitas algo más?
```

---

### Flujo 4 — Cancelar cita: por horario (Sonia Acosta Sep 11)

**Turno 1 — Barbero:** Cancela la cita de las 3:00 p.m. del 11 de septiembre de 2026.

**Turno 2 — IA/Resultado:** Encontré esta cita:

- **Cliente:** Sonia Acosta
- **Fecha:** 11 de septiembre de 2026, 3:00 p.m. – 3:30 p.m.
- **ID:** 4

¿Confirmas que quieres cancelarla?

---

**Turno 3 — Barbero:** Sí, confírmala.

**Turno 4 — IA/Resultado:** Listo. La cita de Sonia Acosta del 11 de septiembre de 2026 a las 3:00 p.m. quedó **cancelada**.

**Estado:** ✅ PASS
**Keywords/condición buscada:** `cancelad, libre, sonia`
**Última respuesta:**
```
Listo. La cita de Sonia Acosta del 11 de septiembre de 2026 a las 3:00 p.m. quedó **cancelada**.
```

---

### Flujo 5 — Cancelar cita: Carlos Cruz (2 citas activas)

**Turno 1 — Barbero:** Cancela la cita de Carlos Cruz.

**Turno 2 — IA/Resultado:** Carlos Cruz tiene 3 citas activas:

1. **14/09/2026** 17:00–17:30 (id 1)
2. **15/09/2026** 18:00–19:00 (id 6)
3. **16/09/2026** 11:00–11:30 (id 2)

¿Cuál deseas cancelar?

---

**Turno 3 — Barbero:** La del 14.

**Turno 4 — IA/Resultado:** Listo. Cita del **14/09/2026 17:00–17:30** de Carlos Cruz cancelada.

Le quedan 2 citas activas: 15/09 18:00 y 16/09 11:00.

---

**Turno 5 — Barbero:** Sí.

**Turno 6 — IA/Resultado:** Verifiqué el estado: la cita del **14/09/2026 17:00** ya no aparece como activa para Carlos Cruz (id 255). Sus 2 citas activas siguen siendo 15/09 18:00 (id 6) y 16/09 11:00 (id 2).

No estoy seguro de a qué te refieres con "Sí". ¿Quieres cancelar alguna de las otras dos, o necesitas algo más?

**Estado:** ✅ PASS
**Keywords/condición buscada:** `cancelad, 14 de septiembre, carlos`
**Última respuesta:**
```
Verifiqué el estado: la cita del **14/09/2026 17:00** ya no aparece como activa para Carlos Cruz (id 255). Sus 2 citas activas siguen siendo 15/09 18:00 (id 6) y 16/09 11:00 (id 2).

No estoy seguro de a qué te refieres con "Sí". ¿Quieres cancelar alguna de las otras dos, o necesitas algo más?
```

---

### Flujo 6 — Reagendar: Luis de la Cruz Roque sin conflicto

**Turno 1 — Barbero:** Mueve la cita de Luis de la Cruz Roque del 14 de septiembre de 2026 a las 6:00 p.m. para el 18 de septiembre de 2026 a la misma hora.

**Turno 2 — IA/Resultado:** Encontré dos clientes con ese nombre:

1. **Luis de la Cruz Roque** — tel. 527298339825 (id 254)
2. **Luis de la Cruz Roque** — tel. 527298339824 (id 253)

¿Cuál es el cliente correcto?

---

**Turno 3 — Barbero:** Sí, muévela.

**Turno 4 — IA/Resultado:** Listo ✅

La cita de **Luis de la Cruz Roque** (id 253) quedó movida:
- **Antes:** 14 sep 2026, 18:00
- **Ahora:** 18 sep 2026, 18:00–18:30

**Estado:** ✅ PASS
**Keywords/condición buscada:** `reubicad, movid, actualiz, 18 de septiembre`
**Última respuesta:**
```
Listo ✅

La cita de **Luis de la Cruz Roque** (id 253) quedó movida:
- **Antes:** 14 sep 2026, 18:00
- **Ahora:** 18 sep 2026, 18:00–18:30
```

---

### Flujo 7 — Reagendar: Marisol Acosta con conflicto en Sep 18 18:00

**Turno 1 — Barbero:** Cambia la cita de Marisol Acosta para el 18 de septiembre de 2026 a las 6:00 p.m.

**Turno 2 — IA/Resultado:** Marisol Acosta (tel. 7717203342) tiene una cita actual el 13 de septiembre de 2026, 4:00–4:45 p.m.

El horario solicitado **18 de septiembre, 6:00 p.m. no está disponible**: ya hay una cita (Luis de la Cruz Roque) de 6:00 a 6:30 p.m.

Opción más cercana: **18 de septiembre, 6:30–7:15 p.m.**

¿Confirmas mover la cita a las 6:30 p.m., o prefieres otro horario?

---

**Turno 3 — Barbero:** A las 6:30 p.m.

**Turno 4 — IA/Resultado:** Listo. Cita de Marisol Acosta movida al **18 de septiembre de 2026, 6:30–7:15 p.m.**

**Estado:** ✅ PASS
**Keywords/condición buscada:** `6:30, actualiz, movid, reubicad, marisol`
**Última respuesta:**
```
Listo. Cita de Marisol Acosta movida al **18 de septiembre de 2026, 6:30–7:15 p.m.**
```

---

### Test A — Búsqueda cruzada por IA no debe encontrar clientes ajenos

**Turno 1 — Barbero:** Busca al cliente Sonia Acosta

**Turno 2 — IA/Resultado:** No encontré coincidencias con "Sonia Acosta". ¿Quieres que busque solo por "Sonia" o me confirmas el nombre completo?

**Estado:** ✅ PASS
**Keywords/condición buscada:** `no encontr, no existe, no tengo registro, no aparece, no hay coincidencia, ningún cliente, ninguna cliente`
**Última respuesta:**
```
No encontré coincidencias con "Sonia Acosta". ¿Quieres que busque solo por "Sonia" o me confirmas el nombre completo?
```

---

### Test B — Crear cita con customerId de otro tenant debe responder 404

**Turno 1 — Barbero:** POST /appointments customerId=252

**Turno 2 — IA/Resultado:** HTTP 404

**Estado:** ✅ PASS
**Keywords/condición buscada:** `404`
**Última respuesta:**
```
HTTP 404
```

---

### Test C — Mismo teléfono en tenant distinto crea un Customer separado

**Turno 1 — Barbero:** POST /customers phone=527714056025 (como miguel)

**Turno 2 — IA/Resultado:** id 302 (OWNER: 252)

**Estado:** ✅ PASS
**Keywords/condición buscada:** `id distinto`
**Última respuesta:**
```
id 302 vs 252
```

---

### Test D — Control positivo: flujo propio de miguel funciona

**Turno 1 — Barbero:** POST /customers + POST /appointments (como miguel)

**Turno 2 — IA/Resultado:** Customer 303, Appointment 7

**Estado:** ✅ PASS
**Keywords/condición buscada:** `ids generados`
**Última respuesta:**
```
Customer 303, Appointment 7
```
