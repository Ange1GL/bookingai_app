import { AppointmentDto } from './appointment.dto';

export type CalendarView = 'month' | 'week';

/** `in-progress` and `finished` are not persisted: they are derived from the clock. */
export type DisplayStatus = 'reserved' | 'in-progress' | 'finished' | 'cancelled';

/** Half-open range [from, to), matching the backend contract. */
export interface DateRange {
  from: Date;
  to: Date;
}

export interface CalendarAppointment {
  id: number;
  start: Date;
  end: Date;
  customerName: string;
  status: DisplayStatus;
  source: AppointmentDto;
}
