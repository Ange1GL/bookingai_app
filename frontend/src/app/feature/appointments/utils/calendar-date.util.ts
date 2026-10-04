import { AppointmentDto, AppointmentStatusId } from '../models/appointment.dto';
import { CalendarAppointment, CalendarView, DateRange, DisplayStatus } from '../models/calendar.model';

export const DAYS_IN_WEEK = 7;
const MONDAY_OFFSET_SUNDAY = 6;
const PAD_LENGTH = 2;
const MS_PER_DAY = 86_400_000;

const pad = (value: number): string => String(value).padStart(PAD_LENGTH, '0');

export const startOfDay = (date: Date): Date => new Date(date.getFullYear(), date.getMonth(), date.getDate());

export const addDays = (date: Date, days: number): Date =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);

/** Weeks start on Monday. */
export const startOfWeek = (date: Date): Date => {
  const daysSinceMonday = (date.getDay() + MONDAY_OFFSET_SUNDAY) % DAYS_IN_WEEK;
  return addDays(startOfDay(date), -daysSinceMonday);
};

export const startOfMonth = (date: Date): Date => new Date(date.getFullYear(), date.getMonth(), 1);

/** Moves by whole months, clamping the day so Jan 31 + 1 month is Feb 28/29. */
export const addMonths = (date: Date, months: number): Date => {
  const target = new Date(date.getFullYear(), date.getMonth() + months, 1);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  return new Date(target.getFullYear(), target.getMonth(), Math.min(date.getDate(), lastDay));
};

export const isSameDay = (a: Date, b: Date): boolean =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

export const isSameMonth = (a: Date, b: Date): boolean =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();

/** `YYYY-MM-DD` in local time; used as a grouping key. */
export const dateKey = (date: Date): string =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

/** `YYYY-MM-DDTHH:mm:ss` in local time, no `Z`: the API works with offset-less LocalDateTime. */
export const toLocalIso = (date: Date): string =>
  `${dateKey(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;

export const buildWeek = (anchor: Date): Date[] => {
  const first = startOfWeek(anchor);
  return Array.from({ length: DAYS_IN_WEEK }, (_, index) => addDays(first, index));
};

/** Full weeks (Mon-Sun) covering the month, including filler days from neighbouring months. */
export const buildMonthGrid = (anchor: Date): Date[] => {
  const monthEnd = new Date(anchor.getFullYear(), anchor.getMonth() + 1, 0);
  const first = startOfWeek(startOfMonth(anchor));
  const last = addDays(startOfWeek(monthEnd), DAYS_IN_WEEK - 1);
  const length = Math.round((last.getTime() - first.getTime()) / MS_PER_DAY) + 1;
  return Array.from({ length }, (_, index) => addDays(first, index));
};

/** The visible range as [from, to): `to` is the midnight after the last visible day. */
export const rangeFor = (view: CalendarView, anchor: Date): DateRange => {
  const days = view === 'month' ? buildMonthGrid(anchor) : buildWeek(anchor);
  return { from: days[0] as Date, to: addDays(days[days.length - 1] as Date, 1) };
};

export const shiftPeriod = (view: CalendarView, anchor: Date, direction: 1 | -1): Date =>
  view === 'month' ? addMonths(anchor, direction) : addDays(anchor, direction * DAYS_IN_WEEK);

export const displayStatus = (dto: Pick<AppointmentDto, 'statusId'>, start: Date, end: Date, now: Date): DisplayStatus => {
  if (dto.statusId === AppointmentStatusId.Cancelled) {
    return 'cancelled';
  }
  if (now >= end) {
    return 'finished';
  }
  return now >= start ? 'in-progress' : 'reserved';
};

export const toCalendarAppointment = (dto: AppointmentDto, now: Date): CalendarAppointment => {
  const start = new Date(dto.startTime);
  const end = new Date(dto.endTime);
  return { id: dto.id, start, end, customerName: dto.customerName, status: displayStatus(dto, start, end, now), source: dto };
};

/** Groups by the day the appointment starts, each group sorted chronologically. */
export const groupByDay = (appointments: CalendarAppointment[]): Map<string, CalendarAppointment[]> => {
  const groups = new Map<string, CalendarAppointment[]>();
  for (const appointment of appointments) {
    const key = dateKey(appointment.start);
    groups.set(key, [...(groups.get(key) ?? []), appointment]);
  }
  for (const list of groups.values()) {
    list.sort((a, b) => a.start.getTime() - b.start.getTime());
  }
  return groups;
};
