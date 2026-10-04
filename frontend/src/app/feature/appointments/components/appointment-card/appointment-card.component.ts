import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CalendarAppointment } from '../../models/calendar.model';
import { STATUS_STYLE } from '../../models/status-style.model';
import { formatTime, formatTimeRange } from '../../utils/calendar-format.util';

@Component({
  selector: 'app-appointment-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="flex min-h-16 w-full cursor-pointer items-stretch gap-3 rounded-2xl border border-surface bg-surface-0 p-3 text-left shadow-sm transition active:scale-[0.99] dark:bg-surface-900"
      [attr.aria-label]="ariaLabel()"
      (click)="selected.emit(appointment())"
    >
      <span class="w-1.5 shrink-0 rounded-full" [class]="style().dot" aria-hidden="true"></span>
      <span class="flex min-w-0 flex-1 flex-col gap-1">
        <span class="truncate font-semibold" [class.line-through]="appointment().status === 'cancelled'">
          {{ appointment().customerName }}
        </span>
        <span class="flex items-center gap-1.5 text-sm text-muted-color">
          <i class="pi pi-clock text-xs" aria-hidden="true"></i>
          {{ range() }}
        </span>
      </span>
      <span class="flex shrink-0 items-center">
        <span class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium" [class]="style().chip">
          <i class="pi text-[10px]" [class]="style().icon" aria-hidden="true"></i>
          {{ style().label }}
        </span>
      </span>
    </button>
  `,
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
