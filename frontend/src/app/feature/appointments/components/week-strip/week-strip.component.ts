import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CalendarAppointment } from '../../models/calendar.model';
import { STATUS_STYLE } from '../../models/status-style.model';
import { dateKey, isSameDay } from '../../utils/calendar-date.util';
import { formatLongDay, WEEKDAYS_SHORT } from '../../utils/calendar-format.util';

const MAX_DOTS = 3;

interface WeekCell {
  date: Date;
  key: string;
  weekday: string;
  day: number;
  isToday: boolean;
  isSelected: boolean;
  dots: CalendarAppointment[];
  ariaLabel: string;
}

@Component({
  selector: 'app-week-strip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './week-strip.component.html',
})
export class WeekStripComponent {
  readonly days = input.required<Date[]>();
  readonly selected = input.required<Date>();
  readonly groups = input.required<Map<string, CalendarAppointment[]>>();
  readonly loading = input(false);
  readonly daySelected = output<Date>();

  protected readonly cells = computed<WeekCell[]>(() => {
    const today = new Date();
    return this.days().map((date, index) => {
      const key = dateKey(date);
      const items = this.groups().get(key) ?? [];
      return {
        date,
        key,
        weekday: WEEKDAYS_SHORT[index] ?? '',
        day: date.getDate(),
        isToday: isSameDay(date, today),
        isSelected: isSameDay(date, this.selected()),
        dots: items.slice(0, MAX_DOTS),
        ariaLabel: `${formatLongDay(date)}, ${items.length === 1 ? '1 cita' : `${items.length} citas`}`,
      };
    });
  });

  protected dotColor(item: CalendarAppointment): string {
    return STATUS_STYLE[item.status].dot;
  }
}
