import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CalendarAppointment } from '../../models/calendar.model';
import { STATUS_STYLE } from '../../models/status-style.model';
import { formatTime, formatTimeRange } from '../../utils/calendar-format.util';

@Component({
  selector: 'app-appointment-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './appointment-card.component.html',
})
export class AppointmentCardComponent {
  readonly appointment = input.required<CalendarAppointment>();
  readonly selected = output<CalendarAppointment>();

  protected readonly style = computed(() => STATUS_STYLE[this.appointment().status]);
  protected readonly range = computed(() => formatTimeRange(this.appointment().start, this.appointment().end));
  protected readonly ariaLabel = computed(
    () => `${this.appointment().customerName}, ${formatTime(this.appointment().start)}, ${this.style().label}`,
  );
}
