import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AppointmentsService } from './appointments.service';

describe('AppointmentsService', () => {
  it('requests one appointment by id', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(AppointmentsService).findById(7).subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/appointments/7`);
    expect(req.request.method).toBe('GET');
    req.flush({});
    http.verify();
  });

  it('posts a new appointment to the collection', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const body = { customerId: 1, priceCatalogId: 2, startTime: '2026-10-20T09:00:00', endTime: '2026-10-20T10:00:00' };

    TestBed.inject(AppointmentsService).create(body).subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/appointments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({});
    http.verify();
  });

  it('cancels an appointment with a POST to its cancel action', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(AppointmentsService).cancel(9).subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/appointments/9/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush({});
    http.verify();
  });

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
