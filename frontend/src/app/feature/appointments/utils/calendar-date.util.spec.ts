import { AppointmentDto } from '../models/appointment.dto';
import {
  addMonths,
  buildMonthGrid,
  dateKey,
  groupByDay,
  rangeFor,
  startOfWeek,
  toCalendarAppointment,
  toLocalIso,
} from './calendar-date.util';

const dto = (id: number, startTime: string, endTime: string, statusId = 1): AppointmentDto => ({
  id,
  startTime,
  endTime,
  customerId: 1,
  customerName: 'Ana',
  status: statusId === 1 ? 'RESERVED' : 'CANCELLED',
  statusId,
  priceCatalogId: 1,
  serviceLabel: 'Corte básico',
  price: 60,
});

describe('calendar-date.util', () => {
  it('starts weeks on Monday, including Sundays', () => {
    expect(dateKey(startOfWeek(new Date(2026, 9, 4)))).toBe('2026-09-28'); // Sunday
    expect(dateKey(startOfWeek(new Date(2026, 9, 5)))).toBe('2026-10-05'); // Monday
  });

  it('builds a month grid of full weeks', () => {
    const grid = buildMonthGrid(new Date(2026, 9, 15)); // October 2026
    expect(grid.length % 7).toBe(0);
    expect(dateKey(grid[0] as Date)).toBe('2026-09-28');
    expect(dateKey(grid[grid.length - 1] as Date)).toBe('2026-11-01');
  });

  it('handles a 4-week month that starts on Monday (Feb 2027)', () => {
    expect(buildMonthGrid(new Date(2027, 1, 10)).length).toBe(28);
  });

  it('uses a half-open range ending at the midnight after the last visible day', () => {
    const { from, to } = rangeFor('week', new Date(2026, 9, 7));
    expect(toLocalIso(from)).toBe('2026-10-05T00:00:00');
    expect(toLocalIso(to)).toBe('2026-10-12T00:00:00');
  });

  it('clamps the day when moving months and crosses years', () => {
    expect(dateKey(addMonths(new Date(2028, 0, 31), 1))).toBe('2028-02-29');
    expect(dateKey(addMonths(new Date(2026, 11, 15), 1))).toBe('2027-01-15');
  });

  it('formats local ISO without a timezone suffix', () => {
    expect(toLocalIso(new Date(2026, 0, 5, 9, 3, 7))).toBe('2026-01-05T09:03:07');
  });

  it('derives the display status from the clock', () => {
    const now = new Date(2026, 9, 4, 10, 30);
    expect(toCalendarAppointment(dto(1, '2026-10-04T11:00:00', '2026-10-04T12:00:00'), now).status).toBe('reserved');
    expect(toCalendarAppointment(dto(2, '2026-10-04T10:00:00', '2026-10-04T11:00:00'), now).status).toBe('in-progress');
    expect(toCalendarAppointment(dto(3, '2026-10-04T08:00:00', '2026-10-04T09:00:00'), now).status).toBe('finished');
    expect(toCalendarAppointment(dto(4, '2026-10-04T11:00:00', '2026-10-04T12:00:00', 2), now).status).toBe('cancelled');
  });

  it('groups by start day sorted by time', () => {
    const now = new Date(2026, 9, 4);
    const groups = groupByDay([
      toCalendarAppointment(dto(1, '2026-10-04T15:00:00', '2026-10-04T16:00:00'), now),
      toCalendarAppointment(dto(2, '2026-10-04T09:00:00', '2026-10-04T10:00:00'), now),
      toCalendarAppointment(dto(3, '2026-10-05T09:00:00', '2026-10-05T10:00:00'), now),
    ]);
    expect(groups.get('2026-10-04')?.map((a) => a.id)).toEqual([2, 1]);
    expect(groups.get('2026-10-05')?.length).toBe(1);
  });
});
