import { CalendarAppointment } from './calendar.model';

/** Groups of the day agenda, in the order they are shown: what is happening, what comes next, then history. */
export type AgendaSectionId = 'in-progress' | 'upcoming' | 'finished' | 'cancelled';

export interface AgendaSection {
  id: AgendaSectionId;
  title: string;
  appointments: CalendarAppointment[];
  /** Whether the day has more than one section; a single one is shown as a plain list, without a header. */
  showHeader: boolean;
  /** Finished appointments fold away while something is still pending, so the next one is always in view. */
  collapsible: boolean;
}
