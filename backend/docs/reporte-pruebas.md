# Reporte de Pruebas — Agente Barbería

**Fecha:** 2026-09-11T22:34:00.888Z
**Entorno:** http://localhost:8082/api/v1
**Modelo:** deepseek-flash
**Delay entre requests:** 1500ms

---

## Resumen

| Métrica | Valor |
|---|---|
| ✅ PASS | 7 |
| ❌ FAIL | 0 |
| Total | 7 |

| Flujo | Estado | Keywords buscadas |
|---|---|---|
| Flujo 1 — Crear cita: Sonia Acosta (cliente existente) | ✅ PASS | `sonia, guardada, agendada, 3:30, cita` |
| Flujo 2 — Crear cita: Marisol Acosta (cliente nueva) | ✅ PASS | `marisol, registrada, guardada, agendada, 4:45` |
| Flujo 3 — Crear cita: ambigüedad "Carlos" | ✅ PASS | `carlos cruz, agendada, guardada, 6:00, 15 de septiembre` |
| Flujo 4 — Cancelar cita: por horario (Sonia Acosta Sep 11) | ✅ PASS | `cancelad, libre, sonia` |
| Flujo 5 — Cancelar cita: Carlos Cruz (2 citas activas) | ✅ PASS | `cancelad, 14 de septiembre, carlos` |
| Flujo 6 — Reagendar: Luis de la Cruz Roque sin conflicto | ✅ PASS | `reubicad, movid, actualiz, 18 de septiembre` |
| Flujo 7 — Reagendar: Marisol Acosta con conflicto en Sep 18 18:00 | ✅ PASS | `6:30, actualiz, movid, reubicad, marisol` |

---

## Detalle por flujo

### Flujo 1 — Crear cita: Sonia Acosta (cliente existente)

**Turno 1 — Barbero:** Agenda una cita para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m.

**Turno 2 — IA:** Listo ✅

- **Cliente:** Sonia Acosta (existente)
- **Fecha:** 11 de septiembre de 2026
- **Horario:** 3:00 p.m. – 3:30 p.m.
- **Cita #:** 23

¿Necesitas algo más?

---

**Turno 3 — Barbero:** Sí, es para ella.

**Turno 4 — IA:** Confirmado: Sonia Acosta existe en la base (id 252) y es una coincidencia única, así que no hace falta aclarar nada.

Además, revisé el horario y ya hay una cita registrada exactamente en ese bloque:

- **Cita #23** — Sonia Acosta — 11/09/2026, 3:00 p.m. a 3:30 p.m. (PENDING)

Es decir, la cita ya estaba agendada, por lo que **no creé una duplicada**. Si quieres, puedo moverla o cancelarla.

**Estado:** ✅ PASS
**Keywords buscadas:** `sonia, guardada, agendada, 3:30, cita`
**Última respuesta IA:**
```
Confirmado: Sonia Acosta existe en la base (id 252) y es una coincidencia única, así que no hace falta aclarar nada.

Además, revisé el horario y ya hay una cita registrada exactamente en ese bloque:

- **Cita #23** — Sonia Acosta — 11/09/2026, 3:00 p.m. a 3:30 p.m. (PENDING)

Es decir, la cita ya estaba agendada, por lo que **no creé una duplicada**. Si quieres, puedo moverla o cancelarla.
```

---

### Flujo 2 — Crear cita: Marisol Acosta (cliente nueva)

**Turno 1 — Barbero:** Agenda una cita para Marisol Acosta el 13 de septiembre de 2026 a las 4:00 p.m.

**Turno 2 — IA:** El horario del 13 de septiembre de 2026 a las 4:00 p.m. está libre. Solo me falta el número de teléfono de Marisol para crearla y confirmar la cita.

---

**Turno 3 — Barbero:** Su número es 771 720 3342.

**Turno 4 — IA:** Listo ✅ Cita agendada:

- **Marisol Acosta** (cliente nuevo, tel. 771 720 3342)
- **13 de septiembre de 2026, 4:00–5:00 p.m.**
- Cita #24

¿Algo más?

---

**Turno 5 — Barbero:** Sí, correcto.

**Turno 6 — IA:** Verificado ✅

- **Marisol Acosta** — ya existía en la base (id 257, tel. 771 720 3342)
- **Cita #24**: 13 de septiembre de 2026, 4:00–5:00 p.m., estado pendiente

Todo quedó registrado correctamente.

---

**Turno 7 — Barbero:** Corte y barba, 45 minutos.

**Turno 8 — IA:** Entendido: **corte y barba (45 min)**. Eso ajustaría la cita #24 a **4:00–4:45 p.m.** del 13 de septiembre de 2026.

¿Confirmo el cambio de horario?

**Estado:** ✅ PASS
**Keywords buscadas:** `marisol, registrada, guardada, agendada, 4:45`
**Última respuesta IA:**
```
Entendido: **corte y barba (45 min)**. Eso ajustaría la cita #24 a **4:00–4:45 p.m.** del 13 de septiembre de 2026.

¿Confirmo el cambio de horario?
```

---

### Flujo 3 — Crear cita: ambigüedad "Carlos"

**Turno 1 — Barbero:** Agenda una cita para Carlos el 15 de septiembre de 2026 a las 6:00 p.m.

**Turno 2 — IA:** Encontré dos clientes con ese nombre:

1. **Carlos Cruz** — tel. 527713905509
2. **Carlos Javier** — tel. 527711240938

¿Cuál de los dos es? Y confirma la duración de la cita (¿hasta las 7:00 p.m.?). El horario de las 6:00 p.m. del 15/09/2026 está libre.

---

**Turno 3 — Barbero:** Es el que termina en 9509.

**Turno 4 — IA:** No hay coincidencia: ningún teléfono termina en 9509.

- **Carlos Cruz** — 527713905509 (termina en **5509**)
- **Carlos Javier** — 527711240938 (termina en **0938**)

¿Quisiste decir 5509 (Carlos Cruz) o me pasas otro dato? Y confirma la hora de fin de la cita (¿7:00 p.m.?).

**Estado:** ✅ PASS
**Keywords buscadas:** `carlos cruz, agendada, guardada, 6:00, 15 de septiembre`
**Última respuesta IA:**
```
No hay coincidencia: ningún teléfono termina en 9509.

- **Carlos Cruz** — 527713905509 (termina en **5509**)
- **Carlos Javier** — 527711240938 (termina en **0938**)

¿Quisiste decir 5509 (Carlos Cruz) o me pasas otro dato? Y confirma la hora de fin de la cita (¿7:00 p.m.?).
```

---

### Flujo 4 — Cancelar cita: por horario (Sonia Acosta Sep 11)

**Turno 1 — Barbero:** Cancela la cita de las 3:00 p.m. del 11 de septiembre de 2026.

**Turno 2 — IA:** Encontré esta cita:

- **Cliente:** Sonia Acosta
- **Fecha:** 11 de septiembre de 2026
- **Hora:** 3:00 p.m. – 3:30 p.m.
- **ID:** 23

¿Confirmas que deseas cancelarla?

---

**Turno 3 — Barbero:** Sí, confírmala.

**Turno 4 — IA:** ✅ Cita cancelada.

- **Cliente:** Sonia Acosta
- **11 de septiembre de 2026, 3:00 p.m. – 3:30 p.m.**
- **Estado:** Cancelada

¿Necesitas agendar algo más?

**Estado:** ✅ PASS
**Keywords buscadas:** `cancelad, libre, sonia`
**Última respuesta IA:**
```
✅ Cita cancelada.

- **Cliente:** Sonia Acosta
- **11 de septiembre de 2026, 3:00 p.m. – 3:30 p.m.**
- **Estado:** Cancelada

¿Necesitas agendar algo más?
```

---

### Flujo 5 — Cancelar cita: Carlos Cruz (2 citas activas)

**Turno 1 — Barbero:** Cancela la cita de Carlos Cruz.

**Turno 2 — IA:** Carlos Cruz tiene 2 citas activas:

1. **14/09/2026** 17:00–17:30 (id 20)
2. **16/09/2026** 11:00–11:30 (id 21)

¿Cuál deseas cancelar?

---

**Turno 3 — Barbero:** La del 14.

**Turno 4 — IA:** ¿Confirmas cancelar la cita del **14/09/2026 17:00–17:30** (id 20)? Esta acción no se puede deshacer.

---

**Turno 5 — Barbero:** Sí.

**Turno 6 — IA:** Listo ✅ La cita del **14/09/2026 17:00–17:30** de Carlos Cruz fue cancelada.

¿Algo más?

**Estado:** ✅ PASS
**Keywords buscadas:** `cancelad, 14 de septiembre, carlos`
**Última respuesta IA:**
```
Listo ✅ La cita del **14/09/2026 17:00–17:30** de Carlos Cruz fue cancelada.

¿Algo más?
```

---

### Flujo 6 — Reagendar: Luis de la Cruz Roque sin conflicto

**Turno 1 — Barbero:** Mueve la cita de Luis de la Cruz Roque del 14 de septiembre de 2026 a las 6:00 p.m. para el 18 de septiembre de 2026 a la misma hora.

**Turno 2 — IA:** Encontré dos clientes con el mismo nombre:

1. **Luis de la Cruz Roque** — tel. 527298339824 (id 253)
2. **Luis de la Cruz Roque** — tel. 527298339825 (id 254)

¿Cuál de los dos es el cliente correcto?

---

**Turno 3 — Barbero:** Sí, muévela.

**Turno 4 — IA:** Listo. La cita quedó movida:

- **Cliente:** Luis de la Cruz Roque (id 253, tel. 527298339824)
- **De:** 14 sep 2026, 6:00 p.m. → **A:** 18 sep 2026, 6:00 p.m.

Nota: había un segundo cliente con el mismo nombre (id 254), pero solo el id 253 tenía esa cita.

**Estado:** ✅ PASS
**Keywords buscadas:** `reubicad, movid, actualiz, 18 de septiembre`
**Última respuesta IA:**
```
Listo. La cita quedó movida:

- **Cliente:** Luis de la Cruz Roque (id 253, tel. 527298339824)
- **De:** 14 sep 2026, 6:00 p.m. → **A:** 18 sep 2026, 6:00 p.m.

Nota: había un segundo cliente con el mismo nombre (id 254), pero solo el id 253 tenía esa cita.
```

---

### Flujo 7 — Reagendar: Marisol Acosta con conflicto en Sep 18 18:00

**Turno 1 — Barbero:** Cambia la cita de Marisol Acosta para el 18 de septiembre de 2026 a las 6:00 p.m.

**Turno 2 — IA:** El horario de las 6:00 p.m. no está disponible: Luis de la Cruz Roque tiene una cita de 18:00 a 18:30 ese día.

Opciones más cercanas para Marisol Acosta (cita actual: 13 sep, 4:00 p.m.):
- 18 sep, 5:00–6:00 p.m. (libre)
- 18 sep, 6:30–7:30 p.m. (después de la cita de Luis)

¿Cuál prefieres? Confirma y la muevo.

---

**Turno 3 — Barbero:** A las 6:30 p.m.

**Turno 4 — IA:** Listo ✅

Marisol Acosta — 18 de septiembre de 2026, 6:30 a 7:30 p.m. (antes: 13 sep, 4:00 p.m.)

**Estado:** ✅ PASS
**Keywords buscadas:** `6:30, actualiz, movid, reubicad, marisol`
**Última respuesta IA:**
```
Listo ✅

Marisol Acosta — 18 de septiembre de 2026, 6:30 a 7:30 p.m. (antes: 13 sep, 4:00 p.m.)
```
