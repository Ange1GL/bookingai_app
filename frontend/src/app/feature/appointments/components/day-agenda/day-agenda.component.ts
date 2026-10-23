import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { CalendarAppointment } from '../../models/calendar.model';
import { AgendaSection } from '../../models/day-agenda.model';
import { buildAgendaSections } from '../../utils/day-agenda.util';
import { formatLongDay } from '../../utils/calendar-format.util';
import { AppointmentCardComponent } from '../appointment-card/appointment-card.component';

const SKELETON_ROWS = [0, 1, 2];

/**
 * The day's appointments grouped by what matters now (in progress, upcoming, finished, cancelled).
 * Finished ones fold away while something is pending, so the next appointment is always in view.
 */
@Component({
  selector: 'app-day-agenda',
  imports: [AppointmentCardComponent, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './day-agenda.component.html',
})
export class DayAgendaComponent {
  readonly date = input.required<Date>();
  readonly appointments = input.required<CalendarAppointment[]>();
  readonly loading = input(false);
  readonly selected = output<CalendarAppointment>();

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly title = computed(() => formatLongDay(this.date()));
  protected readonly sections = computed(() => buildAgendaSections(this.appointments()));
  protected readonly countLabel = computed(() => {
    const count = this.appointments().length;
    return count === 1 ? '1 cita' : `${count} citas`;
  });

  /** Folded again every time another day is picked. */
  protected readonly finishedOpen = linkedSignal<Date, boolean>({ source: this.date, computation: () => false });

  protected toggleFinished(): void {
    this.finishedOpen.update((open) => !open);
  }

  protected isOpen(section: AgendaSection): boolean {
    return !section.collapsible || this.finishedOpen();
  }
}
