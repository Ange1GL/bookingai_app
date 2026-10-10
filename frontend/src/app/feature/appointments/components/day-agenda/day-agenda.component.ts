import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { CalendarAppointment } from '../../models/calendar.model';
import { formatLongDay } from '../../utils/calendar-format.util';
import { AppointmentCardComponent } from '../appointment-card/appointment-card.component';

const SKELETON_ROWS = [0, 1, 2];

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
  protected readonly countLabel = computed(() => {
    const count = this.appointments().length;
    return count === 1 ? '1 cita' : `${count} citas`;
  });
}
