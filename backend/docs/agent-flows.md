# Flujos de prueba — Agente IA Barbería

Clientes de referencia:
- **Luis de la Cruz Roque** — `527298339824`
- **Carlos Cuactemoc** — `527713905509`
- **Sonia Acosta** — `527714056025`
- **Luis de la Cruz Roque (duplicado)** — `527298339825` *(mismo nombre, último dígito cambiado)*

---

## Flujo 1 — Crear cita: cliente existente (coincidencia única)

```
Barbero: Agenda una cita para Mario Baustis el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m.

IA: Encontré a Sonia Acosta con el teléfono +52 111 111 1111. ¿Es para ella la cita o se trata de un cliente nuevo?

Barbero: Sí, es para ella.

IA: Perfecto. Cita guardada para Sonia Acosta el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m.
```

---

## Flujo 2 — Crear cita: cliente nuevo (sin coincidencias)

```
Barbero: Agenda una cita para Pedro Ramírez el 12 de septiembre de 2026 a las 4:00 p.m.

IA: No encontré a ningún Pedro Ramírez en la lista de clientes. Para registrarlo, ¿cuál es su número telefónico?

Barbero: Es 771 234 5678.

IA: Confirmando número: 771 234 5678. ¿Es correcto?

Barbero: Sí, correcto.

IA: Cliente registrado. ¿Qué servicio se realizará y cuántos minutos durará?

Barbero: Corte y barba, 45 minutos.

IA: Cita guardada con éxito para Pedro Ramírez el 12 de septiembre de 2026 de 4:00 p.m. a 4:45 p.m.
```

---

## Flujo 3 — Crear cita: ambigüedad / múltiples coincidencias

```
Barbero: Cita para Luis el 13 de septiembre de 2026 a las 6:00 p.m.

IA: Tengo 2 clientes registrados con el nombre "Luis":
  1. Luis de la Cruz Roque (Tel: ...9824)
  2. Luis de la Cruz Roque (Tel: ...9825)
  ¿Se trata de alguno de ellos o es un cliente nuevo?

Barbero: Es el del teléfono que termina en 9824.

IA: Excelente. Cita agendada para Luis de la Cruz Roque (...9824) el 13 de septiembre de 2026 a las 6:00 p.m.
```

---

## Flujo 4 — Cancelar cita: por horario

```
Barbero: Cancela la cita de las 3:00 p.m. del 11 de septiembre de 2026.

IA: La cita de las 3:00 p.m. pertenece a Sonia Acosta. ¿Confirmas que deseas cancelar esta cita?

Barbero: Sí, confírmala.

IA: Cita cancelada correctamente. El espacio de 3:00 p.m. a 3:30 p.m. del 11 de septiembre de 2026 ha quedado libre en tu agenda.
```

---

## Flujo 5 — Cancelar cita: nombre ambiguo (varias citas)

```
Barbero: Cancela la cita de Carlos Cuactemoc.

IA: Carlos Cuactemoc tiene dos citas agendadas esta semana:
  1. 14 de septiembre de 2026 a las 5:00 p.m.
  2. 16 de septiembre de 2026 a las 11:00 a.m.
  ¿Cuál de las dos deseas cancelar?

Barbero: La del 14.

IA: Entendido. ¿Confirmas la cancelación de la cita de Carlos Cuactemoc para el 14 de septiembre de 2026 a las 5:00 p.m.?

Barbero: Sí.

IA: Cita del 14 de septiembre de 2026 a las 5:00 p.m. cancelada.
```

---

## Flujo 6 — Reagendar: sin conflicto

```
Barbero: Mueve la cita de Luis de la Cruz Roque del 13 de septiembre de 2026 a las 6:00 p.m. para el 15 de septiembre de 2026 a la misma hora.

IA: Verificando disponibilidad para el 15 de septiembre de 2026 a las 6:00 p.m... El horario está libre. ¿Confirmas mover la cita de Luis de la Cruz Roque?

Barbero: Sí, muévela.

IA: Cita reubicada exitosamente para el 15 de septiembre de 2026 de 6:00 p.m. a 6:30 p.m.
```

---

## Flujo 7 — Reagendar: con conflicto de agenda

```
Barbero: Cambia la cita de Sonia Acosta para el 15 de septiembre de 2026 a las 6:00 p.m.

IA: El horario del 15 de septiembre de 2026 a las 6:00 p.m. ya está ocupado por Luis de la Cruz Roque. Tienes disponible a las 6:30 p.m. o a las 7:00 p.m. ¿A qué hora prefieres moverla?

Barbero: A las 6:30 p.m.

IA: Cita de Sonia Acosta actualizada para el 15 de septiembre de 2026 a las 6:30 p.m.
```
