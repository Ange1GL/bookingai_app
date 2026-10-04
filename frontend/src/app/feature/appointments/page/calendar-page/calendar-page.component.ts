import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { AppointmentDetailComponent } from '../../components/appointment-detail/appointment-detail.component';
import { CalendarToolbarComponent } from '../../components/calendar-toolbar/calendar-toolbar.component';
import { DayAgendaComponent } from '../../components/day-agenda/day-agenda.component';
import { MonthGridComponent } from '../../components/month-grid/month-grid.component';
import { WeekStripComponent } from '../../components/week-strip/week-strip.component';
import { CalendarAppointment, CalendarView, DateRange } from '../../models/calendar.model';
import { AppointmentsService } from '../../service/appointments.service';
import {
  buildMonthGrid,
  buildWeek,
  dateKey,
  groupByDay,
  rangeFor,
  shiftPeriod,
  startOfDay,
  toCalendarAppointment,
} from '../../utils/calendar-date.util';
import { formatPeriodTitle } from '../../utils/calendar-format.util';

const SWIPE_MIN_DISTANCE_PX = 60;
const SWIPE_MAX_VERTICAL_DRIFT_PX = 50;

const sameRange = (a: DateRange, b: DateRange): boolean =>
  a.from.getTime() === b.from.getTime() && a.to.getTime() === b.to.getTime();

@Component({
  selector: 'app-calendar-page',
  imports: [
    ButtonModule,
    CalendarToolbarComponent,
    MonthGridComponent,
    WeekStripComponent,
    DayAgendaComponent,
    AppointmentDetailComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './calendar-page.component.html',
})
export class CalendarPageComponent {
  private readonly appointmentsService = inject(AppointmentsService);

  protected readonly view = signal<CalendarView>('month');
  protected readonly selectedDate = signal(startOfDay(new Date()));
  protected readonly detail = signal<CalendarAppointment | null>(null);
  protected readonly detailVisible = signal(false);

  private swipeStart: { x: number; y: number } | null = null;

  /** Only changes when the visible period changes, so picking a day inside it never refetches. */
  private readonly range = computed(() => rangeFor(this.view(), this.selectedDate()), { equal: sameRange });

  private readonly resource = rxResource({
    params: () => this.range(),
    stream: ({ params }) => this.appointmentsService.findByDateRange(params.from, params.to),
  });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);

  protected readonly title = computed(() => formatPeriodTitle(this.view(), this.selectedDate()));
  protected readonly monthDays = computed(() => buildMonthGrid(this.selectedDate()));
  protected readonly weekDays = computed(() => buildWeek(this.selectedDate()));

  protected readonly groups = computed(() => {
    const now = new Date();
    return groupByDay((this.resource.value() ?? []).map((dto) => toCalendarAppointment(dto, now)));
  });
  protected readonly dayAppointments = computed(() => this.groups().get(dateKey(this.selectedDate())) ?? []);

  protected selectDay(date: Date): void {
    this.selectedDate.set(startOfDay(date));
  }

  protected goToToday(): void {
    this.selectedDate.set(startOfDay(new Date()));
  }

  protected shift(direction: 1 | -1): void {
    this.selectedDate.update((date) => shiftPeriod(this.view(), date, direction));
  }

  protected changeView(view: CalendarView): void {
    this.view.set(view);
  }

  protected openDetail(appointment: CalendarAppointment): void {
    this.detail.set(appointment);
    this.detailVisible.set(true);
  }

  protected retry(): void {
    this.resource.reload();
  }

  protected onTouchStart(event: TouchEvent): void {
    const touch = event.touches[0];
    this.swipeStart = touch ? { x: touch.clientX, y: touch.clientY } : null;
  }

  protected onTouchEnd(event: TouchEvent): void {
    const touch = event.changedTouches[0];
    const start = this.swipeStart;
    this.swipeStart = null;
    if (!touch || !start) {
      return;
    }
    const deltaX = touch.clientX - start.x;
    const deltaY = Math.abs(touch.clientY - start.y);
    if (Math.abs(deltaX) >= SWIPE_MIN_DISTANCE_PX && deltaY <= SWIPE_MAX_VERTICAL_DRIFT_PX) {
      this.shift(deltaX < 0 ? 1 : -1);
    }
  }
}
