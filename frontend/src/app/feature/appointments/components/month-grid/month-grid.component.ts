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
  template: `
    <div class="grid grid-cols-7 text-center text-xs font-semibold uppercase tracking-wide text-muted-color" aria-hidden="true">
      @for (name of weekdays; track $index) {
        <span class="py-2">{{ name }}</span>
      }
    </div>

    <div class="grid grid-cols-7 gap-px md:gap-1" role="grid" [class.opacity-60]="loading()">
      @for (cell of cells(); track cell.key) {
        <button
          type="button"
          role="gridcell"
          class="relative flex min-h-12 cursor-pointer flex-col items-center gap-1 rounded-xl p-1 transition md:min-h-28 md:items-stretch md:p-2"
          [class]="cellClass(cell)"
          [attr.aria-label]="cell.ariaLabel"
          [attr.aria-pressed]="cell.isSelected"
          [attr.aria-current]="cell.isToday ? 'date' : null"
          (click)="daySelected.emit(cell.date)"
        >
          <span
            class="flex size-7 items-center justify-center rounded-full text-sm font-semibold"
            [class]="cell.isToday && !cell.isSelected ? 'bg-primary text-primary-contrast' : ''"
          >
            {{ cell.day }}
          </span>

          <span class="flex h-1.5 items-center gap-0.5 md:hidden" aria-hidden="true">
            @for (item of cell.dots; track item.id) {
              <span class="size-1.5 rounded-full" [class]="cell.isSelected ? 'bg-white' : dotColor(item)"></span>
            }
          </span>

          <span class="hidden w-full flex-col gap-1 md:flex" aria-hidden="true">
            @for (item of cell.chips; track item.id) {
              <span class="truncate rounded-md px-1.5 py-0.5 text-left text-xs font-medium" [class]="chipColor(item)">
                {{ time(item) }} {{ item.customerName }}
              </span>
            }
            @if (cell.extra > 0) {
              <span class="px-1 text-left text-xs text-muted-color" [class.text-white]="cell.isSelected">+{{ cell.extra }} más</span>
            }
          </span>
        </button>
      }
    </div>
  `,
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
