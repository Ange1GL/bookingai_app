import { LegalSection } from './legal-section';

export interface PrivacyHighlight {
  title: string;
  description: string;
  icon: string;
}

export const PRIVACY_LAST_UPDATE = '14 de octubre de 2026';

export const PRIVACY_HIGHLIGHTS: PrivacyHighlight[] = [
  {
    title: 'Solo lo necesario',
    description: 'Guardamos los datos que hacen falta para operar tu agenda, sin publicidad ni analítica de terceros.',
    icon: 'pi-check-circle',
  },
  {
    title: 'Contraseñas protegidas',
    description: 'Tu contraseña se almacena cifrada con BCrypt (hash), nunca en texto legible.',
    icon: 'pi-lock',
  },
  {
    title: 'Datos separados por cuenta',
    description: 'Cada negocio ve únicamente sus propios clientes, citas y precios.',
    icon: 'pi-users',
  },
  {
    title: 'Tú decides',
    description: 'Puedes pedir acceso, corrección o eliminación de tus datos escribiéndonos por correo.',
    icon: 'pi-user-edit',
  },
];

export const PRIVACY_SECTIONS: LegalSection[] = [
  {
    title: '1. Quiénes somos y alcance',
    paragraphs: [
      'aglacita es un software como servicio (SaaS) para administrar citas. Esta política explica qué datos tratamos, para qué y cómo los protegemos cuando usas el servicio.',
      'Tus datos de cuenta los tratamos nosotros como responsables. Los datos de tus clientes (nombre y teléfono) los registras tú en tu agenda: tú decides qué capturas y eres quien debe contar con el derecho o consentimiento para hacerlo; nosotros los procesamos únicamente para prestarte el servicio.',
    ],
  },
  {
    title: '2. Datos que tratamos',
    items: [
      'Cuenta: nombre de usuario, correo electrónico, nombre y contraseña (almacenada como hash BCrypt).',
      'Agenda: clientes (nombre y teléfono), citas (fecha, hora, estado y servicio) y tu catálogo de servicios con precios.',
      'Lista negra: los clientes que bloqueas y, si lo indicas, el motivo (máximo 250 caracteres).',
      'Sesión y seguridad: un identificador cifrado de tu sesión, la dirección IP y el navegador (user agent) desde donde iniciaste sesión, y el conteo de intentos fallidos de acceso.',
      'Asistente de AI: los mensajes que escribes en el chat y los datos de tu agenda que el asistente consulta para responderte.',
    ],
  },
  {
    title: '3. Para qué usamos tus datos',
    items: [
      'Crear y mantener tu cuenta y permitirte iniciar sesión.',
      'Mostrar y administrar tu calendario, clientes, catálogo y lista negra.',
      'Responder y ejecutar tus indicaciones en el asistente de AI (agendar, mover o cancelar citas).',
      'Proteger el servicio: limitar intentos repetidos, bloquear temporalmente la cuenta tras 3 intentos fallidos y detectar usos indebidos.',
      'Atender tus solicitudes de información o soporte.',
    ],
    closing: ['No vendemos tus datos ni los de tus clientes, y no los usamos para publicidad.'],
  },
  {
    title: '4. Cookies y almacenamiento',
    paragraphs: [
      'Usamos únicamente cookies funcionales para mantener tu sesión: una de acceso (dura 15 minutos) y una de renovación (hasta 30 días). Son HttpOnly, por lo que el código de la página no puede leerlas, usan SameSite y, en el entorno en línea, el atributo Secure. También usamos una cookie de protección contra solicitudes falsas (CSRF).',
      'No usamos cookies de publicidad ni herramientas de analítica o seguimiento. Cerrar sesión revoca tus tokens en nuestro servidor.',
    ],
  },
  {
    title: '5. Con quién se comparten',
    paragraphs: ['Para operar el servicio nos apoyamos en proveedores que procesan datos por nuestra cuenta:'],
    items: [
      'Amazon Web Services (AWS): alojamiento de la aplicación y de la información (CloudFront, API Gateway, ECS Fargate y S3).',
      'DeepSeek: proveedor del modelo de lenguaje del asistente de AI. Cuando usas el chat, se le envían tus mensajes y los datos de agenda necesarios para responderte, como nombres y teléfonos de clientes y datos de citas. Evita escribir información sensible que no sea necesaria para agendar.',
      'Google Fonts: al cargar la página, tu navegador descarga la tipografía desde Google, que puede recibir tu dirección IP.',
    ],
  },
  {
    title: '6. Cómo protegemos tus datos',
    items: [
      'Contraseñas almacenadas con hash BCrypt; nunca se guardan ni se registran en texto legible.',
      'Sesión mediante cookies HttpOnly, tokens de vida corta y revocación al cerrar sesión.',
      'Aislamiento por cuenta: cada consulta se filtra por el usuario autenticado para que un negocio no acceda a los datos de otro.',
      'Límite de solicitudes por dirección IP y bloqueo temporal de cuentas tras intentos fallidos.',
      'Los secretos de la aplicación se mantienen fuera del código fuente.',
    ],
    closing: [
      'Ningún sistema es infalible; aplicamos estas medidas para reducir riesgos, pero no podemos garantizar seguridad absoluta.',
    ],
  },
  {
    title: '7. Conservación',
    paragraphs: [
      'Conservamos tus datos mientras tu cuenta esté activa o sea necesario para prestarte el servicio. Los tokens de sesión expiran (hasta 30 días) y los tokens revocados se depuran automáticamente. Las conversaciones del asistente se mantienen en la memoria del servidor para dar continuidad al chat y no se guardan en la base de datos.',
    ],
  },
  {
    title: '8. Tus derechos',
    paragraphs: [
      'Puedes solicitar el acceso, la rectificación o la cancelación de tus datos personales, oponerte a su tratamiento o revocar tu consentimiento. Escríbenos desde el correo de tu cuenta al contacto indicado abajo y te responderemos a la brevedad.',
    ],
  },
  {
    title: '9. Menores de edad',
    paragraphs: ['aglacita está dirigido a negocios y personas adultas; no está pensado para menores de edad.'],
  },
  {
    title: '10. Cambios a esta política',
    paragraphs: [
      'Si cambiamos la forma en que tratamos los datos, actualizaremos esta página e indicaremos la fecha de la última actualización.',
    ],
  },
];
