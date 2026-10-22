import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, model, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, Subject, switchMap } from 'rxjs';
import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { CustomerFormDialogComponent } from '@/shared/components/customer-form-dialog/customer-form-dialog.component';
import { CreateCustomerRequestDto } from '@/shared/models/customer-input.model';
import { DateTimeFieldComponent } from '../date-time-field/date-time-field.component';
import { CreateAppointmentRequestDto } from '../../models/appointment.dto';
import { AppointmentForm } from '../../models/appointment-form.model';
import { CustomerOptionDto, ServiceOptionDto } from '../../models/booking-options.model';
import { BookingLookupService } from '../../service/booking-lookup.service';
import {
  addMinutes,
  DEFAULT_DURATION_MINUTES,
  endAfterStart,
  endFollowingStart,
  suggestedStart,
  toCreateRequest,
} from '../../utils/appointment-form.util';
import { notBefore } from '../../utils/date-time.util';

@Component({
  selector: 'app-appointment-form-dialog',
  imports: [
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    AutoCompleteModule,
    SelectModule,
    DateTimeFieldComponent,
    CustomerFormDialogComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './appointment-form-dialog.component.html',
})
export class AppointmentFormDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly lookup = inject(BookingLookupService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly customerQuery = new Subject<string>();
  private lastStart: Date | null = null;

  readonly visible = model(false);
  readonly saving = input(false);
  /** Day highlighted in the calendar; the form suggests a start time inside it. */
  readonly day = input<Date>(new Date());
  readonly submitted = output<CreateAppointmentRequestDto>();

  protected readonly minDate = signal(new Date());
  protected readonly customers = signal<CustomerOptionDto[]>([]);
  protected readonly services = signal<ServiceOptionDto[]>([]);
  protected readonly loadingServices = signal(false);
  protected readonly customerFormVisible = signal(false);
  protected readonly creatingCustomer = signal(false);

  protected readonly form: FormGroup<AppointmentForm> = this.fb.group(
    {
      customer: this.fb.control<CustomerOptionDto | null>(null, Validators.required),
      service: this.fb.control<ServiceOptionDto | null>(null, Validators.required),
      start: this.fb.control<Date | null>(null, [Validators.required, notBefore(() => this.minDate())]),
      end: this.fb.control<Date | null>(null, Validators.required),
    },
    { validators: endAfterStart },
  );

  constructor() {
    this.customerQuery
      .pipe(
        // Errors are toasted by errorInterceptor; an empty list keeps the search usable.
        switchMap((term) => this.lookup.searchCustomers(term).pipe(catchError(() => of([])))),
        takeUntilDestroyed(),
      )
      .subscribe((found) => this.customers.set(found));

    this.form.controls.start.valueChanges.pipe(takeUntilDestroyed()).subscribe((start) => this.moveEnd(start));
  }

  protected onShow(): void {
    const now = new Date();
    // Start of the current minute: a start before the moment the dialog opened is in the past.
    now.setSeconds(0, 0);
    this.minDate.set(now);
    const start = suggestedStart(this.day(), now);
    this.lastStart = start;
    this.form.reset({ start, end: addMinutes(start, DEFAULT_DURATION_MINUTES) });
    this.customers.set([]);
    this.loadServices();
  }

  protected createCustomer(request: CreateCustomerRequestDto): void {
    this.creatingCustomer.set(true);
    this.lookup
      .createCustomer(request)
      .pipe(
        finalize(() => this.creatingCustomer.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (customer) => {
          this.form.controls.customer.setValue(customer);
          this.customerFormVisible.set(false);
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  protected searchCustomers(event: AutoCompleteCompleteEvent): void {
    this.customerQuery.next(event.query);
  }

  protected submit(): void {
    const request = this.form.valid ? toCreateRequest(this.form.getRawValue()) : null;
    if (!request) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(request);
  }

  private loadServices(): void {
    this.loadingServices.set(true);
    this.lookup
      .listServices()
      .pipe(
        finalize(() => this.loadingServices.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (services) => this.services.set(services),
        // errorInterceptor already shows the toast.
        error: () => undefined,
      });
  }

  /**
   * The end keeps its duration when the start changes. This also fires on every keystroke while typing a time
   * ("5", "5:1", "5:13"): shifting by a fixed duration lands on the right end whatever the intermediate values.
   */
  private moveEnd(start: Date | null): void {
    if (!start) {
      return;
    }
    const { end } = this.form.controls;
    end.setValue(endFollowingStart(this.lastStart, start, end.value));
    this.lastStart = start;
  }
}
