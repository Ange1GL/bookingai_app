import { CalendarView } from '../models/calendar.model';
import { buildWeek, DAYS_IN_WEEK } from './calendar-date.util';

const LOCALE = 'es-MX';

const capitalize = (text: string): string => text.charAt(0).toUpperCase() + text.slice(1);

const monthYear = new Intl.DateTimeFormat(LOCALE, { month: 'long', year: 'numeric' });
const dayMonth = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short' });
const dayMonthYear = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', year: 'numeric' });
const longDay = new Intl.DateTimeFormat(LOCALE, { weekday: 'long', day: 'numeric', month: 'long' });
const weekdayShort = new Intl.DateTimeFormat(LOCALE, { weekday: 'short' });
const weekdayLong = new Intl.DateTimeFormat(LOCALE, { weekday: 'long' });
const time = new Intl.DateTimeFormat(LOCALE, { hour: '2-digit', minute: '2-digit', hour12: false });

export const formatTime = (date: Date): string => time.format(date);

export const formatTimeRange = (start: Date, end: Date): string => `${formatTime(start)} – ${formatTime(end)}`;

export const formatLongDay = (date: Date): string => capitalize(longDay.format(date));

export const formatPeriodTitle = (view: CalendarView, anchor: Date): string => {
  if (view === 'month') {
    return capitalize(monthYear.format(anchor));
  }
  const week = buildWeek(anchor);
  const first = week[0] as Date;
  const last = week[DAYS_IN_WEEK - 1] as Date;
  return `${dayMonth.format(first)} – ${dayMonthYear.format(last)}`;
};

/** Weekday names Monday-first, in short and long forms. */
const reference = buildWeek(new Date(2026, 0, 5));
export const WEEKDAYS_SHORT: string[] = reference.map((day) => capitalize(weekdayShort.format(day).replace('.', '')));
export const WEEKDAYS_LONG: string[] = reference.map((day) => capitalize(weekdayLong.format(day)));
