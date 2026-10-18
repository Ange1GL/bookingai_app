import { FormControl, FormGroup } from '@angular/forms';
import { CustomerOptionDto, ServiceOptionDto } from '../models/booking-options.model';
import { endAfterStart, suggestedStart, toCreateRequest } from './appointment-form.util';
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

  it('routes digits to phone and text to name', () => {
    expect(toCustomerSearchParams(' 55 12 ')).toEqual({ phone: '55 12' });
    expect(toCustomerSearchParams('Ana')).toEqual({ name: 'Ana' });
    expect(toCustomerSearchParams('  ')).toEqual({});
  });
});
