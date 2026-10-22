import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const TIME_PATTERN = /^([01]\d|2[0-3]):([0-5]\d)$/;
const PAD_LENGTH = 2;

export interface TimeOfDay {
  hours: number;
  minutes: number;
}

/** "17:13" in 24 h format; anything else (partial like "17:_", "25:70", empty) is not a time. */
export const parseTime = (text: string | null | undefined): TimeOfDay | null => {
  const match = TIME_PATTERN.exec(text ?? '');
  return match ? { hours: Number(match[1]), minutes: Number(match[2]) } : null;
};

export const formatTime = (date: Date): string =>
  `${String(date.getHours()).padStart(PAD_LENGTH, '0')}:${String(date.getMinutes()).padStart(PAD_LENGTH, '0')}`;

/** Day from `date` (its own time is ignored) plus the typed `time`; `null` until both are valid. */
export const combineDateAndTime = (date: Date | null, time: string | null | undefined): Date | null => {
  const parsed = parseTime(time);
  if (!date || !parsed) {
    return null;
  }
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), parsed.hours, parsed.minutes);
};

/** Fails with `inPast` when the value is before the limit (read on every validation, so it can change). */
export const notBefore =
  (limit: () => Date): ValidatorFn =>
  (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as Date | null;
    return value && value < limit() ? { inPast: true } : null;
  };
