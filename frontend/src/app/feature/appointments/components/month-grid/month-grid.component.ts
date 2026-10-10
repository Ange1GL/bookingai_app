import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CalendarAppointment } from '../../models/calendar.model';
import { STATUS_STYLE } from '../../models/status-style.model';
import { dateKey, isSameDay, isSameMonth } from '../../utils/calendar-date.util';
import { formatLongDay, formatTime, WEEKDAYS_SHORT } from '../../utils/calendar-format.util';

const MAX_DOTS = 3;
const MAX_CHIPS = 2;

interface MonthCell {
  date: Date;
  key: string;
  day: number;
  inMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  items: CalendarAppointment[];
  dots: CalendarAppointment[];
  chips: CalendarAppointment[];
  extra: number;
  ariaLabel: string;
}

@Component({
  selector: 'app-month-grid',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './month-grid.component.html',
})
export class MonthGridComponent {
  readonly days = input.required<Date[]>();
  readonly month = input.required<Date>();
  readonly selected = input.required<Date>();
  readonly groups = input.required<Map<string, CalendarAppointment[]>>();
  readonly loading = input(false);
  readonly daySelected = output<Date>();

  protected readonly weekdays = WEEKDAYS_SHORT;

  protected readonly cells = computed<MonthCell[]>(() => {
    const today = new Date();
    return this.days().map((date) => {
      const key = dateKey(date);
      const items = this.groups().get(key) ?? [];
      const count = items.length;
      return {
        date,
        key,
        day: date.getDate(),
        inMonth: isSameMonth(date, this.month()),
        isToday: isSameDay(date, today),
        isSelected: isSameDay(date, this.selected()),
        items,
        dots: items.slice(0, MAX_DOTS),
        chips: items.slice(0, MAX_CHIPS),
        extra: Math.max(0, count - MAX_CHIPS),
        ariaLabel: `${formatLongDay(date)}, ${count === 1 ? '1 cita' : `${count} citas`}`,
      };
    });
  });

  protected cellClass(cell: MonthCell): string {
    if (cell.isSelected) {
      return 'bg-linear-to-br from-primary-500 to-primary-700 text-white shadow-md shadow-primary-500/30';
    }
    return cell.inMonth ? 'hover:bg-surface-100 dark:hover:bg-surface-800' : 'text-muted-color opacity-50 hover:bg-surface-100 dark:hover:bg-surface-800';
  }

  protected dotColor(item: CalendarAppointment): string {
    return STATUS_STYLE[item.status].dot;
  }

  protected chipColor(item: CalendarAppointment): string {
    return STATUS_STYLE[item.status].event;
  }

  protected time(item: CalendarAppointment): string {
    return formatTime(item.start);
  }
}
