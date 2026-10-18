import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { NEW_APPOINTMENT_QUERY_PARAM } from '@/shared/navigation/appointment-links';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { ButtonModule } from 'primeng/button';
import { AppointmentDetailComponent } from '../../components/appointment-detail/appointment-detail.component';
import { AppointmentFormDialogComponent } from '../../components/appointment-form-dialog/appointment-form-dialog.component';
import { CalendarToolbarComponent } from '../../components/calendar-toolbar/calendar-toolbar.component';
import { DayAgendaComponent } from '../../components/day-agenda/day-agenda.component';
import { MonthGridComponent } from '../../components/month-grid/month-grid.component';
import { WeekStripComponent } from '../../components/week-strip/week-strip.component';
import { AppointmentDto, CreateAppointmentRequestDto } from '../../models/appointment.dto';
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
import { formatLongDay, formatPeriodTitle, formatTime } from '../../utils/calendar-format.util';

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
    AppointmentFormDialogComponent,
    ConfirmDialogComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './calendar-page.component.html',
})
export class CalendarPageComponent {
  private readonly appointmentsService = inject(AppointmentsService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly formVisible = signal(false);
  protected readonly creating = signal(false);
  protected readonly cancelVisible = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly cancelTarget = signal<CalendarAppointment | null>(null);
  protected readonly cancelMessage = computed(() => {
    const target = this.cancelTarget();
    return target
      ? `Se cancelará la cita de ${target.customerName} del ${formatLongDay(target.start)} a las ${formatTime(target.start)}. Esta acción no se puede deshacer.`
      : '';
  });
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

  constructor() {
    this.openFormFromLink();
  }

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

  protected askCancel(appointment: CalendarAppointment): void {
    this.cancelTarget.set(appointment);
    this.cancelVisible.set(true);
  }

  protected confirmCancel(): void {
    const target = this.cancelTarget();
    if (!target) {
      return;
    }
    this.cancelling.set(true);
    this.appointmentsService
      .cancel(target.id)
      .pipe(
        finalize(() => this.cancelling.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.cancelVisible.set(false);
          this.detailVisible.set(false);
          this.messages.add({ severity: 'success', summary: 'Cita cancelada', detail: target.customerName });
          this.resource.reload();
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  /** `?nueva=true` (e.g. from the assistant's fallback) opens the form once and is then removed from the URL. */
  private openFormFromLink(): void {
    if (!this.route.snapshot.queryParamMap.has(NEW_APPOINTMENT_QUERY_PARAM)) {
      return;
    }
    this.formVisible.set(true);
    void this.router.navigate([], {
      queryParams: { [NEW_APPOINTMENT_QUERY_PARAM]: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  protected openForm(): void {
    this.formVisible.set(true);
  }

  protected create(request: CreateAppointmentRequestDto): void {
    this.creating.set(true);
    this.appointmentsService
      .create(request)
      .pipe(
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (appointment) => this.onCreated(appointment),
        // errorInterceptor already shows the toast; keep the dialog open so the user can adjust and retry.
        error: () => undefined,
      });
  }

  /** Jumps to the booked day so the new appointment is visible; refetches only if that day was already on screen. */
  private onCreated(appointment: AppointmentDto): void {
    this.formVisible.set(false);
    this.messages.add({ severity: 'success', summary: 'Cita agendada', detail: `${appointment.customerName} · ${appointment.serviceLabel}` });
    const before = this.range();
    this.selectDay(new Date(appointment.startTime));
    if (sameRange(before, this.range())) {
      this.resource.reload();
    }
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
