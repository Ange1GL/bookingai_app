import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AppointmentsService } from './appointments.service';

describe('AppointmentsService', () => {
  it('requests the range with local ISO params and no trailing Z', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    let result: unknown;

    TestBed.inject(AppointmentsService)
      .findByDateRange(new Date(2026, 9, 5), new Date(2026, 9, 12))
      .subscribe((appointments) => (result = appointments));

    const req = http.expectOne((r) => r.url === `${environment.apiBaseUrl}/api/v1/appointments`);
    expect(req.request.params.get('from')).toBe('2026-10-05T00:00:00');
    expect(req.request.params.get('to')).toBe('2026-10-12T00:00:00');
    req.flush([]);
    expect(result).toEqual([]);
    http.verify();
  });
});
