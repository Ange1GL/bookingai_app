import { CalendarAppointment, DisplayStatus } from '../models/calendar.model';
import { AgendaSection, AgendaSectionId } from '../models/day-agenda.model';

const SECTION_BY_STATUS: Record<DisplayStatus, AgendaSectionId> = {
  'in-progress': 'in-progress',
  reserved: 'upcoming',
  finished: 'finished',
  cancelled: 'cancelled',
};

/** Display order of the sections. */
const SECTION_ORDER: readonly AgendaSectionId[] = ['in-progress', 'upcoming', 'finished', 'cancelled'];

const SECTION_TITLE: Record<AgendaSectionId, string> = {
  'in-progress': 'En curso',
  upcoming: 'Próximas',
  finished: 'Finalizadas',
  cancelled: 'Canceladas',
};

const byStartAscending = (a: CalendarAppointment, b: CalendarAppointment): number => a.start.getTime() - b.start.getTime();

/** The one that just ended comes first: the most recent history is what the owner looks back at. */
const byMostRecentlyFinished = (a: CalendarAppointment, b: CalendarAppointment): number =>
  b.end.getTime() - a.end.getTime() || b.start.getTime() - a.start.getTime();

const SORT_BY_SECTION: Record<AgendaSectionId, (a: CalendarAppointment, b: CalendarAppointment) => number> = {
  'in-progress': byStartAscending,
  upcoming: byStartAscending,
  finished: byMostRecentlyFinished,
  cancelled: byStartAscending,
};

const isPending = (id: AgendaSectionId): boolean => id === 'in-progress' || id === 'upcoming';

/**
 * Orders a day's appointments by what matters now: in progress, then upcoming (the next one first), then
 * finished (most recent first) and cancelled. Empty sections are left out, and a day with a single section
 * (a past or a future day) comes back as one plain list without a header.
 */
export const buildAgendaSections = (appointments: readonly CalendarAppointment[]): AgendaSection[] => {
  const present = SECTION_ORDER.map((id) => ({
    id,
    appointments: appointments.filter((appointment) => SECTION_BY_STATUS[appointment.status] === id).sort(SORT_BY_SECTION[id]),
  })).filter((section) => section.appointments.length > 0);

  const hasPending = present.some((section) => isPending(section.id));
  const showHeader = present.length > 1;
  return present.map((section) => ({
    ...section,
    title: SECTION_TITLE[section.id],
    showHeader,
    collapsible: section.id === 'finished' && hasPending,
  }));
};
