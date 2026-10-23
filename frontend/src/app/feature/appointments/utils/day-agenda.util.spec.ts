import { AppointmentDto } from '../models/appointment.dto';
import { CalendarAppointment, DisplayStatus } from '../models/calendar.model';
import { buildAgendaSections } from './day-agenda.util';

let nextId = 1;

function appointment(name: string, status: DisplayStatus, startHour: number, endHour = startHour + 1): CalendarAppointment {
  return {
    id: nextId++,
    start: new Date(2026, 9, 23, startHour),
    end: new Date(2026, 9, 23, endHour),
    customerName: name,
    status,
    source: {} as AppointmentDto,
  };
}

const names = (appointments: CalendarAppointment[]): string[] => appointments.map((a) => a.customerName);

describe('buildAgendaSections', () => {
  it('shows in progress, then upcoming, then finished, then cancelled', () => {
    const sections = buildAgendaSections([
      appointment('cancelada', 'cancelled', 12),
      appointment('terminada', 'finished', 9),
      appointment('proxima', 'reserved', 16),
      appointment('ahora', 'in-progress', 14),
    ]);

    expect(sections.map((s) => s.id)).toEqual(['in-progress', 'upcoming', 'finished', 'cancelled']);
    expect(sections.map((s) => s.title)).toEqual(['En curso', 'Próximas', 'Finalizadas', 'Canceladas']);
  });

  it('puts the next appointment first among the upcoming ones', () => {
    const [upcoming] = buildAgendaSections([
      appointment('tarde', 'reserved', 18),
      appointment('siguiente', 'reserved', 15),
      appointment('noche', 'reserved', 20),
    ]);

    expect(names(upcoming?.appointments ?? [])).toEqual(['siguiente', 'tarde', 'noche']);
  });

  it('lists finished appointments from the most recent to the oldest', () => {
    const sections = buildAgendaSections([
      appointment('manana', 'finished', 9, 10),
      appointment('mediodia', 'finished', 12, 13),
      appointment('recien', 'finished', 13, 14),
      appointment('pendiente', 'reserved', 18),
    ]);

    const finished = sections.find((s) => s.id === 'finished');
    expect(names(finished?.appointments ?? [])).toEqual(['recien', 'mediodia', 'manana']);
  });

  it('breaks a tie between finished appointments by the later start', () => {
    const [finished] = buildAgendaSections([appointment('larga', 'finished', 9, 11), appointment('corta', 'finished', 10, 11)]);

    expect(names(finished?.appointments ?? [])).toEqual(['corta', 'larga']);
  });

  it('folds finished appointments only while something is pending', () => {
    const withUpcoming = buildAgendaSections([appointment('a', 'finished', 9), appointment('b', 'reserved', 18)]);
    expect(withUpcoming.find((s) => s.id === 'finished')?.collapsible).toBe(true);

    const withInProgress = buildAgendaSections([appointment('a', 'finished', 9), appointment('b', 'in-progress', 10, 12)]);
    expect(withInProgress.find((s) => s.id === 'finished')?.collapsible).toBe(true);

    const onlyHistory = buildAgendaSections([appointment('a', 'finished', 9), appointment('b', 'cancelled', 11)]);
    expect(onlyHistory.find((s) => s.id === 'finished')?.collapsible).toBe(false);
  });

  it('never folds the pending sections', () => {
    const sections = buildAgendaSections([appointment('a', 'in-progress', 9, 12), appointment('b', 'reserved', 18)]);

    expect(sections.every((s) => !s.collapsible)).toBe(true);
  });

  it('returns a single plain list for a past or a future day', () => {
    const past = buildAgendaSections([appointment('a', 'finished', 9), appointment('b', 'finished', 11)]);
    const future = buildAgendaSections([appointment('c', 'reserved', 9), appointment('d', 'reserved', 11)]);

    expect(past).toHaveLength(1);
    expect(past[0]?.showHeader).toBe(false);
    expect(past[0]?.collapsible).toBe(false);
    expect(future).toHaveLength(1);
    expect(future[0]?.showHeader).toBe(false);
  });

  it('shows headers as soon as there is more than one section', () => {
    const sections = buildAgendaSections([appointment('a', 'reserved', 18), appointment('b', 'cancelled', 11)]);

    expect(sections.every((s) => s.showHeader)).toBe(true);
  });

  it('leaves out empty sections and handles an empty day', () => {
    expect(buildAgendaSections([])).toEqual([]);
    expect(buildAgendaSections([appointment('a', 'reserved', 18)]).map((s) => s.id)).toEqual(['upcoming']);
  });

  it('does not modify the list it receives', () => {
    const input = [appointment('tarde', 'reserved', 18), appointment('temprano', 'reserved', 9)];

    buildAgendaSections(input);

    expect(names(input)).toEqual(['tarde', 'temprano']);
  });
});
