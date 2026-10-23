import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppointmentDto } from '../../models/appointment.dto';
import { CalendarAppointment, DisplayStatus } from '../../models/calendar.model';
import { DayAgendaComponent } from './day-agenda.component';

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

describe('DayAgendaComponent', () => {
  let fixture: ComponentFixture<DayAgendaComponent>;
  let el: HTMLElement;

  function render(appointments: CalendarAppointment[], date = new Date(2026, 9, 23)): void {
    fixture = TestBed.createComponent(DayAgendaComponent);
    fixture.componentRef.setInput('date', date);
    fixture.componentRef.setInput('appointments', appointments);
    fixture.detectChanges();
    el = fixture.nativeElement as HTMLElement;
  }

  const customerNames = (): string[] =>
    Array.from(el.querySelectorAll('app-appointment-card .font-semibold')).map((n) => n.textContent?.trim() ?? '');
  const finishedToggle = (): HTMLButtonElement | null => el.querySelector('button[aria-expanded]');

  const mixedDay = (): CalendarAppointment[] => [
    appointment('Terminada', 'finished', 9),
    appointment('Siguiente', 'reserved', 17),
    appointment('Ahora', 'in-progress', 14, 16),
    appointment('Cancelada', 'cancelled', 12),
  ];

  it('shows the pending appointments first, with the finished ones folded', () => {
    render(mixedDay());

    expect(customerNames()).toEqual(['Ahora', 'Siguiente', 'Cancelada']);
    expect(el.textContent).toContain('Finalizadas (1)');
    expect(finishedToggle()?.getAttribute('aria-expanded')).toBe('false');
  });

  it('unfolds the finished appointments below the pending ones on click', () => {
    render(mixedDay());

    finishedToggle()?.click();
    fixture.detectChanges();

    expect(finishedToggle()?.getAttribute('aria-expanded')).toBe('true');
    expect(customerNames()).toEqual(['Ahora', 'Siguiente', 'Terminada', 'Cancelada']);
  });

  it('folds the finished appointments again when another day is picked', () => {
    render(mixedDay());
    finishedToggle()?.click();
    fixture.detectChanges();

    fixture.componentRef.setInput('date', new Date(2026, 9, 24));
    fixture.detectChanges();

    expect(finishedToggle()?.getAttribute('aria-expanded')).toBe('false');
  });

  it('lists a past day as a single plain list, most recent first, without headers or folding', () => {
    render([appointment('Temprano', 'finished', 9), appointment('Tarde', 'finished', 15)]);

    expect(customerNames()).toEqual(['Tarde', 'Temprano']);
    expect(finishedToggle()).toBeNull();
    expect(el.querySelector('h3')).toBeNull();
  });

  it('keeps the next appointment in view when a day has nothing in progress', () => {
    render([appointment('Terminada', 'finished', 9), appointment('Siguiente', 'reserved', 17)]);

    expect(customerNames()).toEqual(['Siguiente']);
  });

  it('shows the count and the empty state', () => {
    render(mixedDay());
    expect(el.textContent).toContain('4 citas');

    render([]);
    expect(el.textContent).toContain('Sin citas este día');
  });

  it('shows skeletons instead of sections while loading', () => {
    fixture = TestBed.createComponent(DayAgendaComponent);
    fixture.componentRef.setInput('date', new Date(2026, 9, 23));
    fixture.componentRef.setInput('appointments', mixedDay());
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('p-skeleton').length).toBe(3);
    expect(fixture.nativeElement.querySelector('app-appointment-card')).toBeNull();
  });
});
