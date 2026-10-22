import { FormControl } from '@angular/forms';
import { combineDateAndTime, formatTime, notBefore, parseTime } from './date-time.util';

describe('date-time util', () => {
  describe('parseTime', () => {
    it('accepts any minute of a 24 h time', () => {
      expect(parseTime('17:13')).toEqual({ hours: 17, minutes: 13 });
      expect(parseTime('05:11')).toEqual({ hours: 5, minutes: 11 });
      expect(parseTime('00:00')).toEqual({ hours: 0, minutes: 0 });
      expect(parseTime('23:59')).toEqual({ hours: 23, minutes: 59 });
    });

    it('rejects partial, out-of-range and empty values', () => {
      for (const text of ['17:1_', '1_:__', '__:__', '25:00', '12:60', '5:13', '', null, undefined]) {
        expect(parseTime(text), String(text)).toBeNull();
      }
    });
  });

  it('formats a date as HH:mm with leading zeros', () => {
    expect(formatTime(new Date(2026, 9, 23, 5, 3))).toBe('05:03');
    expect(formatTime(new Date(2026, 9, 23, 17, 13))).toBe('17:13');
  });

  describe('combineDateAndTime', () => {
    it('takes the day from the date and the time from the text', () => {
      expect(combineDateAndTime(new Date(2026, 9, 23, 9, 45, 30), '17:13')).toEqual(new Date(2026, 9, 23, 17, 13));
    });

    it('is null until both parts are valid', () => {
      expect(combineDateAndTime(null, '17:13')).toBeNull();
      expect(combineDateAndTime(new Date(2026, 9, 23), '17:1_')).toBeNull();
    });
  });

  it('flags a value before the limit', () => {
    const validator = notBefore(() => new Date(2026, 9, 23, 10, 0));

    expect(validator(new FormControl(new Date(2026, 9, 23, 9, 59)))).toEqual({ inPast: true });
    expect(validator(new FormControl(new Date(2026, 9, 23, 10, 0)))).toBeNull();
    expect(validator(new FormControl(null))).toBeNull();
  });
});
