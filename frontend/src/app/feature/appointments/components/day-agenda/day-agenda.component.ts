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
  template: `
    <section class="flex flex-col gap-3" [attr.aria-label]="'Citas del ' + title()">
      <header class="flex items-baseline justify-between gap-2">
        <h2 class="m-0 text-base font-semibold">{{ title() }}</h2>
        @if (!loading()) {
          <span class="text-sm text-muted-color">{{ countLabel() }}</span>
        }
      </header>

      @if (loading()) {
        @for (row of skeletonRows; track row) {
          <p-skeleton height="4rem" borderRadius="1rem" />
        }
      } @else if (appointments().length === 0) {
        <div
          class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-surface px-4 py-8 text-center text-muted-color"
        >
          <span class="flex size-12 items-center justify-center rounded-full bg-primary-50 text-primary dark:bg-primary-500/10">
            <i class="pi pi-calendar text-xl" aria-hidden="true"></i>
          </span>
          <p class="m-0 font-medium">Sin citas este día</p>
          <p class="m-0 text-sm">Disfruta tu tiempo libre.</p>
        </div>
      } @else {
        <ul class="m-0 flex list-none flex-col gap-2 p-0">
          @for (appointment of appointments(); track appointment.id) {
            <li>
              <app-appointment-card [appointment]="appointment" (selected)="selected.emit($event)" />
            </li>
          }
        </ul>
      }
    </section>
  `,
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
