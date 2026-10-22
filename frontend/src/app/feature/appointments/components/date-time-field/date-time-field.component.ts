import { ChangeDetectionStrategy, Component, forwardRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { DatePickerModule } from 'primeng/datepicker';
import { InputMaskModule } from 'primeng/inputmask';
import { DateTimeParts } from '../../models/date-time-field.model';
import { combineDateAndTime, formatTime, parseTime } from '../../utils/date-time.util';

/**
 * Date + time as one form value (`Date | null`). The day comes from a date picker and the time is typed
 * (`HH:mm`, e.g. 17:13) in a masked input: the picker's own time spinners only move by steps, which makes
 * custom minutes slow to reach. The value is `null` until the date and a complete valid time are set, so
 * half-typed times ("17:1") never reach the form.
 */
@Component({
  selector: 'app-date-time-field',
  imports: [ReactiveFormsModule, DatePickerModule, InputMaskModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './date-time-field.component.html',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DateTimeFieldComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => DateTimeFieldComponent), multi: true },
  ],
})
export class DateTimeFieldComponent implements ControlValueAccessor, Validator {
  private readonly fb = inject(NonNullableFormBuilder);

  /** Id of the date input, so the parent `<label for>` points at it. */
  readonly inputId = input.required<string>();
  /** Earliest selectable day (the time is validated by the parent form). */
  readonly minDate = input<Date | null>(null);
  readonly timeLabel = input('Hora');

  protected readonly parts: FormGroup<DateTimeParts> = this.fb.group({
    date: this.fb.control<Date | null>(null),
    time: this.fb.control(''),
  });

  private onChange: (value: Date | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private onValidatorChange: () => void = () => undefined;

  constructor() {
    this.parts.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.onChange(this.currentValue());
      this.onValidatorChange();
    });
  }

  writeValue(value: Date | null): void {
    this.parts.setValue({ date: value, time: value ? formatTime(value) : '' }, { emitEvent: false });
  }

  registerOnChange(fn: (value: Date | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.parts.disable({ emitEvent: false });
    } else {
      this.parts.enable({ emitEvent: false });
    }
  }

  /** A typed time that is not a real 24 h time (25:70) is flagged; an empty one is left to `required`. */
  validate(_control: AbstractControl): ValidationErrors | null {
    const { time } = this.parts.getRawValue();
    const incomplete = time.replaceAll(/[_:\s]/g, '').length > 0 && !parseTime(time);
    return incomplete ? { invalidTime: true } : null;
  }

  protected markTouched(): void {
    this.onTouched();
  }

  private currentValue(): Date | null {
    const { date, time } = this.parts.getRawValue();
    return combineDateAndTime(date, time);
  }
}
