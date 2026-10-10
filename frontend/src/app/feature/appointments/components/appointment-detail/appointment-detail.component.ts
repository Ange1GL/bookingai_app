import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, model } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { DrawerModule } from 'primeng/drawer';
import { SkeletonModule } from 'primeng/skeleton';
import { CalendarAppointment } from '../../models/calendar.model';
import { STATUS_STYLE } from '../../models/status-style.model';
import { AppointmentsService } from '../../service/appointments.service';
import { formatLongDay, formatTimeRange } from '../../utils/calendar-format.util';

const MS_PER_MINUTE = 60_000;
const MINUTES_PER_HOUR = 60;

@Component({
  selector: 'app-appointment-detail',
  imports: [DrawerModule, SkeletonModule, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './appointment-detail.component.html',
})
export class AppointmentDetailComponent {
  readonly appointment = input<CalendarAppointment | null>(null);
  readonly visible = model(false);

  private readonly appointmentsService = inject(AppointmentsService);

  /** Fetches the detail endpoint each time the drawer opens, so the price is never stale. */
  private readonly detailResource = rxResource({
    params: () => (this.visible() ? this.appointment()?.id : undefined),
    stream: ({ params }) => this.appointmentsService.findById(params),
  });

  protected readonly loading = this.detailResource.isLoading;
  protected readonly full = computed(() => {
    const detail = this.detailResource.hasValue() ? this.detailResource.value() : undefined;
    return detail?.id === this.appointment()?.id ? detail : undefined;
  });

  protected readonly style = computed(() => STATUS_STYLE[this.appointment()?.status ?? 'reserved']);
  protected readonly day = computed(() => {
    const item = this.appointment();
    return item ? formatLongDay(item.start) : '';
  });
  protected readonly range = computed(() => {
    const item = this.appointment();
    return item ? formatTimeRange(item.start, item.end) : '';
  });
  protected readonly duration = computed(() => {
    const item = this.appointment();
    if (!item) {
      return '';
    }
    const minutes = Math.max(0, Math.round((item.end.getTime() - item.start.getTime()) / MS_PER_MINUTE));
    const hours = Math.floor(minutes / MINUTES_PER_HOUR);
    const rest = minutes % MINUTES_PER_HOUR;
    return [hours > 0 ? `${hours} h` : '', rest > 0 || hours === 0 ? `${rest} min` : ''].filter(Boolean).join(' ');
  });
}
