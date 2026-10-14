import { LegalSection } from './legal-section';

export const TERMS_LAST_UPDATE = '14 de octubre de 2026';

export const TERMS_SECTIONS: LegalSection[] = [
  {
    title: '1. Aceptación',
    paragraphs: [
      'Estas condiciones regulan el uso de aglacita, un software como servicio (SaaS) para administrar citas. Al iniciar sesión y usar el servicio aceptas estas condiciones y nuestra política de privacidad.',
    ],
  },
  {
    title: '2. Qué es aglacita',
    paragraphs: ['aglacita te ofrece, para tu propio negocio:'],
    items: [
      'Un calendario de citas con vista mensual y semanal.',
      'Un registro de tus clientes (nombre y teléfono) y una lista negra para bloquear a quien decidas.',
      'Un catálogo de servicios y precios; cada cita se asocia a un servicio de tu catálogo.',
      'Un asistente de AI en chat de texto para agendar, mover o cancelar citas.',
    ],
    closing: [
      'aglacita no envía recordatorios ni notificaciones a tus clientes. Las citas tienen dos estados: reservada o cancelada.',
    ],
  },
  {
    title: '3. Cuenta y acceso',
    items: [
      'El acceso al servicio es por inicio de sesión con la cuenta que se te proporciona; no existe registro público. Para adquirir el servicio escríbenos al correo de contacto.',
      'Tu cuenta es personal. Mantén tu contraseña en secreto y avísanos si sospechas un uso no autorizado.',
      'Eres responsable de lo que ocurra desde tu cuenta.',
      'Por seguridad, tras 3 intentos fallidos de inicio de sesión la cuenta se bloquea temporalmente por 30 minutos.',
    ],
  },
  {
    title: '4. Uso aceptable',
    paragraphs: ['Te comprometes a usar el servicio de forma lícita y a no:'],
    items: [
      'Acceder a datos de otras cuentas ni intentar eludir los controles de seguridad.',
      'Enviar solicitudes automatizadas o masivas que afecten el servicio. Existe un límite de solicitudes por dirección IP y, al superarlo, se rechazan temporalmente.',
      'Registrar datos de terceros sin tener derecho o consentimiento para hacerlo.',
      'Usar el servicio para fines ilegales, fraudulentos o para hostigar a otras personas.',
    ],
  },
  {
    title: '5. Tus clientes y tus datos',
    paragraphs: [
      'Los datos de tus clientes que captures siguen siendo tuyos. Tú decides qué registras y eres responsable de contar con el derecho o consentimiento para hacerlo y de avisarles cómo los usas. Nosotros los tratamos únicamente para prestarte el servicio, según nuestra política de privacidad.',
      'Cada cuenta solo ve sus propios clientes, citas y precios.',
    ],
  },
  {
    title: '6. Citas y lista negra',
    items: [
      'No se pueden agendar citas en el pasado ni en un horario que se empalme con otra cita de tu agenda.',
      'Al bloquear a un cliente en la lista negra, ese cliente ya no puede reservar y sus citas futuras se cancelan. Puedes quitarlo de la lista cuando quieras.',
      'Bloquear a un cliente es una decisión tuya; aglacita no bloquea clientes por su cuenta.',
    ],
  },
  {
    title: '7. Asistente de AI',
    paragraphs: [
      'El asistente usa un modelo de lenguaje de un tercero (DeepSeek) y puede equivocarse o malinterpretar una indicación. Revisa en tu calendario que las citas queden como esperas. Antes de eliminar o mover una cita el asistente te pide confirmación, pero la decisión final es tuya.',
      'Evita escribir en el chat información sensible que no sea necesaria para agendar. Consulta la política de privacidad para saber qué datos se envían.',
    ],
  },
  {
    title: '8. Disponibilidad y cambios en el servicio',
    paragraphs: [
      'Trabajamos para que el servicio esté disponible, pero no garantizamos que funcione sin interrupciones ni sin errores. Podemos realizar mantenimientos, mejoras o cambios en las funciones; el asistente puede no estar disponible temporalmente si el proveedor del modelo falla.',
    ],
  },
  {
    title: '9. Precio y pago',
    paragraphs: [
      'Las condiciones comerciales (precio y forma de pago) se acuerdan contigo al adquirir el servicio por correo electrónico y no se cobran dentro de la aplicación.',
    ],
  },
  {
    title: '10. Propiedad intelectual',
    paragraphs: [
      'El software, el nombre aglacita y sus logotipos pertenecen a sus titulares. Se te otorga un derecho personal, limitado y no transferible para usar el servicio mientras tengas acceso; no puedes copiarlo, revenderlo ni intentar extraer su código.',
    ],
  },
  {
    title: '11. Suspensión y terminación',
    paragraphs: [
      'Puedes dejar de usar el servicio en cualquier momento y pedirnos la eliminación de tus datos por correo. Podemos suspender o cerrar una cuenta que incumpla estas condiciones o ponga en riesgo la seguridad del servicio.',
    ],
  },
  {
    title: '12. Limitación de responsabilidad',
    paragraphs: [
      'aglacita es una herramienta de apoyo para organizar tu agenda. En la medida permitida por la ley, no respondemos por pérdidas indirectas, como citas no atendidas o ingresos no percibidos, derivadas de errores de captura, del asistente de AI o de interrupciones del servicio.',
    ],
  },
  {
    title: '13. Cambios y ley aplicable',
    paragraphs: [
      'Podemos actualizar estas condiciones; publicaremos la versión vigente en esta página con su fecha de actualización. Si sigues usando el servicio después de un cambio, se entiende que lo aceptas. Estas condiciones se rigen por las leyes aplicables de México.',
    ],
  },
];
