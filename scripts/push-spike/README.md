# Spike de Web Push (fase 0)

Sirve para confirmar, en tus dispositivos reales, que una notificación push del sistema llega con la web cerrada.
**No es código de producción.** Plan: `.plans/appointment-push-reminders.md`.

```bash
npm install
npm start        # http://localhost:4300 (claves VAPID en .vapid.json, ignorado por git)
```

## Probar

1. Abre la página, pulsa **1. Activar notificaciones** y acepta el permiso.
2. Pulsa **2. Enviar prueba en 10 s**, **cierra la pestaña/app o bloquea la pantalla** y espera.
3. Debe aparecer *"En 5 min: Juan Pérez"* como notificación del sistema. Al tocarla se abre la página.

El bloque "Diagnóstico" de la página muestra qué falta (HTTPS, instalación en iPhone, permiso).

## Por dispositivo

| Dispositivo | Cómo abrirla |
|---|---|
| **Escritorio (Chrome/Edge)** | `http://localhost:4300` directamente (localhost cuenta como contexto seguro). No sirve el navegador embebido de Claude: no soporta service workers ni permisos |
| **Android por USB** | Activa la depuración USB, en el PC abre `chrome://inspect/#devices`, "Port forwarding" `4300 → localhost:4300`, y abre `http://localhost:4300` en el Chrome del teléfono |
| **iPhone / Android por internet** | Requiere HTTPS real: expón el puerto con un túnel temporal (p. ej. `cloudflared tunnel --url http://localhost:4300`). En iPhone abre la URL en Safari → Compartir → **Añadir a pantalla de inicio** → abre **desde el icono** y entonces pulsa el botón 1 |

Detén el túnel y el servidor al terminar: mientras estén abiertos cualquiera con la URL puede enviar una push de prueba a los dispositivos suscritos.
