import { FormControl, FormGroup } from '@angular/forms';
import { CustomerOptionDto, ServiceOptionDto } from '../models/booking-options.model';
import { endAfterStart, endFollowingStart, suggestedStart, toCreateRequest } from './appointment-form.util';
import { toCustomerSearchParams } from './customer-search.util';

const customer: CustomerOptionDto = { id: 4, name: 'Ana', phone: '5512345678', blacklisted: false };
const service: ServiceOptionDto = { id: 2, label: 'Corte', price: 150 };

describe('appointment form util', () => {
  it('builds the request in local time without seconds or Z', () => {
    const request = toCreateRequest({
      customer,
      service,
      start: new Date(2026, 9, 20, 9, 30, 45),
      end: new Date(2026, 9, 20, 10, 30, 12),
    });

    expect(request).toEqual({
      customerId: 4,
      priceCatalogId: 2,
      startTime: '2026-10-20T09:30:00',
      endTime: '2026-10-20T10:30:00',
    });
  });

  it('returns null while the form is incomplete', () => {
    expect(toCreateRequest({ customer, service: null, start: new Date(), end: new Date() })).toBeNull();
  });

  it('suggests the next full hour today and 09:00 on other days', () => {
    const now = new Date(2026, 9, 20, 14, 20);

    expect(suggestedStart(new Date(2026, 9, 20), now)).toEqual(new Date(2026, 9, 20, 15));
    expect(suggestedStart(new Date(2026, 9, 25), now)).toEqual(new Date(2026, 9, 25, 9));
  });

  it('flags an end that is not after the start', () => {
    const group = (start: Date, end: Date) => new FormGroup({ start: new FormControl(start), end: new FormControl(end) });
    const start = new Date(2026, 9, 20, 10);

    expect(endAfterStart(group(start, start))).toEqual({ endBeforeStart: true });
    expect(endAfterStart(group(start, new Date(2026, 9, 20, 11)))).toBeNull();
  });

  describe('endFollowingStart', () => {
    const at = (hours: number, minutes = 0) => new Date(2026, 9, 23, hours, minutes);

    it('keeps the current duration when the start moves', () => {
      expect(endFollowingStart(at(9), at(9, 11), at(10))).toEqual(at(10, 11));
      expect(endFollowingStart(at(9), at(14, 30), at(10, 30))).toEqual(at(16)); // 1 h 30 min de duración
    });

    it('lands on the same end whatever the intermediate values typed on the way', () => {
      let previous = at(9);
      let end = at(10);
      for (const typed of [at(1), at(1, 7), at(17, 1), at(17, 13)]) {
        end = endFollowingStart(previous, typed, end);
        previous = typed;
      }

      expect(end).toEqual(at(18, 13));
    });

    it('uses the default hour when there was no valid duration', () => {
      expect(endFollowingStart(null, at(17, 13), null)).toEqual(at(18, 13));
      expect(endFollowingStart(at(9), at(17, 13), at(9))).toEqual(at(18, 13));
    });
  });

  it('routes digits to phone and text to name', () => {
    expect(toCustomerSearchParams(' 55 12 ')).toEqual({ phone: '55 12' });
    expect(toCustomerSearchParams('Ana')).toEqual({ name: 'Ana' });
    expect(toCustomerSearchParams('  ')).toEqual({});
  });
});
