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
  template: `
    <p-drawer [(visible)]="visible" position="bottom" header="Detalle de la cita" styleClass="h-auto! rounded-t-3xl md:max-w-lg md:mx-auto">
      @if (appointment(); as item) {
        <div class="flex flex-col gap-4 pb-4">
          <div class="flex items-center gap-3">
            <span
              class="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-linear-to-br from-primary-400 to-primary-700 text-white"
              aria-hidden="true"
            >
              <i class="pi pi-user text-xl"></i>
            </span>
            <div class="min-w-0">
              <p class="m-0 truncate text-lg font-semibold" [class.line-through]="item.status === 'cancelled'">
                {{ item.customerName }}
              </p>
              <span class="mt-1 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium" [class]="style().chip">
                <i class="pi text-[10px]" [class]="style().icon" aria-hidden="true"></i>
                {{ style().label }}
              </span>
            </div>
          </div>

          <dl class="m-0 grid grid-cols-[auto_1fr] items-center gap-x-3 gap-y-3 text-sm">
            <dt class="flex items-center gap-2 text-muted-color"><i class="pi pi-calendar" aria-hidden="true"></i>Fecha</dt>
            <dd class="m-0 font-medium">{{ day() }}</dd>
            <dt class="flex items-center gap-2 text-muted-color"><i class="pi pi-clock" aria-hidden="true"></i>Horario</dt>
            <dd class="m-0 font-medium">{{ range() }}</dd>
            <dt class="flex items-center gap-2 text-muted-color"><i class="pi pi-stopwatch" aria-hidden="true"></i>Duración</dt>
            <dd class="m-0 font-medium">{{ duration() }}</dd>
            <dt class="flex items-center gap-2 text-muted-color"><i class="pi pi-tag" aria-hidden="true"></i>Servicio</dt>
            <dd class="m-0 font-medium">
              @if (full(); as detail) {
                {{ detail.serviceLabel }}
              } @else if (loading()) {
                <p-skeleton width="8rem" height="1rem" />
              } @else {
                <span class="text-muted-color">No disponible</span>
              }
            </dd>
            <dt class="flex items-center gap-2 text-muted-color"><i class="pi pi-wallet" aria-hidden="true"></i>Precio</dt>
            <dd class="m-0 font-semibold tabular-nums text-primary">
              @if (full(); as detail) {
                {{ detail.price | currency: 'MXN' : 'symbol-narrow' : '1.0-0' }}
              } @else if (loading()) {
                <p-skeleton width="4rem" height="1rem" />
              } @else {
                <span class="font-normal text-muted-color">No disponible</span>
              }
            </dd>
          </dl>
        </div>
      }
    </p-drawer>
  `,
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
