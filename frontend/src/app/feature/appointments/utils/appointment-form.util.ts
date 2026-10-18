import { AbstractControl, ValidationErrors } from '@angular/forms';
import { CreateAppointmentRequestDto } from '../models/appointment.dto';
import { AppointmentFormValue } from '../models/appointment-form.model';
import { toLocalIso } from './calendar-date.util';

export const DEFAULT_DURATION_MINUTES = 60;
export const DEFAULT_START_HOUR = 9;
const MS_PER_MINUTE = 60_000;

/** The API stores whole minutes; drop seconds the date picker may carry over. */
const withoutSeconds = (date: Date): Date => {
  const copy = new Date(date);
  copy.setSeconds(0, 0);
  return copy;
};

export const addMinutes = (date: Date, minutes: number): Date => new Date(date.getTime() + minutes * MS_PER_MINUTE);

/** Next full hour when booking for today, otherwise the usual opening hour of the chosen day. */
export const suggestedStart = (day: Date, now: Date): Date => {
  const isToday =
    day.getFullYear() === now.getFullYear() && day.getMonth() === now.getMonth() && day.getDate() === now.getDate();
  if (isToday) {
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() + 1);
  }
  return new Date(day.getFullYear(), day.getMonth(), day.getDate(), DEFAULT_START_HOUR);
};

/** Group validator: the end must come after the start. */
export const endAfterStart = (group: AbstractControl): ValidationErrors | null => {
  const start = group.get('start')?.value as Date | null;
  const end = group.get('end')?.value as Date | null;
  return start && end && end <= start ? { endBeforeStart: true } : null;
};

/** Returns `null` while the form is incomplete, so callers never send a partial request. */
export const toCreateRequest = (value: AppointmentFormValue): CreateAppointmentRequestDto | null => {
  const { customer, service, start, end } = value;
  if (!customer || !service || !start || !end) {
    return null;
  }
  return {
    customerId: customer.id,
    priceCatalogId: service.id,
    startTime: toLocalIso(withoutSeconds(start)),
    endTime: toLocalIso(withoutSeconds(end)),
  };
};
