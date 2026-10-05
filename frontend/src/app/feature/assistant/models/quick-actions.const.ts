import { QuickAction } from './chat.model';

/** Typical barbershop-agenda requests; the agent asks for whatever data is missing. */
export const QUICK_ACTIONS: QuickAction[] = [
  { label: 'Agendar cita', description: 'Reserva un horario para un cliente', icon: 'pi-calendar-plus', prompt: 'Quiero agendar una cita' },
  { label: 'Citas de hoy', description: 'Revisa tu agenda del día', icon: 'pi-calendar', prompt: '¿Qué citas tengo hoy?' },
  { label: 'Mover cita', description: 'Cambia el día u hora', icon: 'pi-arrow-right-arrow-left', prompt: 'Quiero mover una cita' },
  { label: 'Cancelar cita', description: 'Libera un horario', icon: 'pi-calendar-times', prompt: 'Quiero cancelar una cita' },
];

/** Short answers that the agent's confirmation questions usually need. */
export const REPLY_CHIPS: QuickAction[] = [
  { label: 'Sí, confirmar', description: '', icon: 'pi-check', prompt: 'Sí, confirma' },
  { label: 'No', description: '', icon: 'pi-times', prompt: 'No' },
  { label: 'Hoy', description: '', icon: 'pi-sun', prompt: 'Hoy' },
  { label: 'Mañana', description: '', icon: 'pi-forward', prompt: 'Mañana' },
  { label: 'Citas de hoy', description: '', icon: 'pi-calendar', prompt: '¿Qué citas tengo hoy?' },
];
