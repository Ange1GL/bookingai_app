import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { SessionService } from '@/core/service/session.service';
import { AppointmentStatusId } from '@/feature/appointments/models/appointment.dto';
import { AppointmentsService } from '@/feature/appointments/service/appointments.service';
import { addDays, startOfDay } from '@/feature/appointments/utils/calendar-date.util';
import { formatLongDay } from '@/feature/appointments/utils/calendar-format.util';

interface UpcomingShortcut {
  label: string;
  icon: string;
}

const UPCOMING_SHORTCUTS: UpcomingShortcut[] = [
  { label: 'Clientes', icon: 'pi-users' },
  { label: 'Nueva cita', icon: 'pi-plus-circle' },
  { label: 'Ajustes', icon: 'pi-cog' },
];

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto flex w-full max-w-4xl flex-col gap-5">
      <section
        class="relative overflow-hidden rounded-3xl bg-linear-to-br from-primary-500 via-primary-600 to-primary-800 p-6 text-white shadow-xl shadow-primary-500/25 md:p-8"
      >
        <span class="pointer-events-none absolute -top-10 -right-10 size-44 rounded-full bg-white/10" aria-hidden="true"></span>
        <span class="pointer-events-none absolute -bottom-16 right-16 size-40 rounded-full bg-white/10" aria-hidden="true"></span>
        <div class="relative flex flex-col gap-1">
          <p class="m-0 text-sm font-medium text-white/80">{{ today }}</p>
          <h1 class="m-0 text-2xl font-bold tracking-tight md:text-3xl">Hola, {{ session.user()?.username }} 👋</h1>
          <p class="m-0 mt-2 inline-flex w-fit items-center gap-2 rounded-full bg-white/15 px-3 py-1.5 text-sm backdrop-blur">
            <i class="pi pi-calendar-clock" aria-hidden="true"></i>
            {{ summary() }}
          </p>
        </div>
      </section>

      <a
        routerLink="/appointments"
        class="group relative flex items-center gap-4 overflow-hidden rounded-3xl border border-surface bg-surface-0 p-5 no-underline shadow-sm transition hover:shadow-lg active:scale-[0.99] dark:bg-surface-900"
      >
        <span
          class="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-linear-to-br from-primary-400 to-primary-700 text-white shadow-lg shadow-primary-500/30 transition group-hover:scale-105"
          aria-hidden="true"
        >
          <i class="pi pi-calendar text-2xl"></i>
        </span>
        <span class="flex min-w-0 flex-1 flex-col">
          <span class="text-lg font-semibold text-color">Mi calendario</span>
          <span class="text-sm text-muted-color">Revisa y administra tus citas del mes o la semana.</span>
        </span>
        <i class="pi pi-arrow-right text-primary transition group-hover:translate-x-1" aria-hidden="true"></i>
      </a>

      <section aria-label="Próximamente">
        <h2 class="m-0 mb-3 text-sm font-semibold uppercase tracking-wide text-muted-color">Próximamente</h2>
        <ul class="m-0 grid list-none grid-cols-3 gap-3 p-0">
          @for (item of shortcuts; track item.label) {
            <li
              class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-surface bg-surface-0 px-2 py-4 text-center text-muted-color opacity-80 dark:bg-surface-900"
            >
              <span class="flex size-11 items-center justify-center rounded-xl bg-primary-50 text-primary dark:bg-primary-500/10" aria-hidden="true">
                <i class="pi text-lg" [class]="item.icon"></i>
              </span>
              <span class="text-xs font-medium sm:text-sm">{{ item.label }}</span>
            </li>
          }
        </ul>
      </section>
    </div>
  `,
})
export class HomePageComponent {
  protected readonly session = inject(SessionService);
  private readonly appointmentsService = inject(AppointmentsService);

  protected readonly shortcuts = UPCOMING_SHORTCUTS;
  protected readonly today = formatLongDay(new Date());

  private readonly todayAppointments = rxResource({
    stream: () => {
      const start = startOfDay(new Date());
      return this.appointmentsService.findByDateRange(start, addDays(start, 1));
    },
  });

  protected readonly summary = computed(() => {
    if (this.todayAppointments.isLoading()) {
      return 'Cargando tu agenda…';
    }
    if (this.todayAppointments.error()) {
      return 'Abre tu calendario para ver tus citas';
    }
    const active = (this.todayAppointments.value() ?? []).filter((a) => a.statusId !== AppointmentStatusId.Cancelled).length;
    if (active === 0) {
      return 'No tienes citas hoy';
    }
    return active === 1 ? 'Tienes 1 cita hoy' : `Tienes ${active} citas hoy`;
  });
}
