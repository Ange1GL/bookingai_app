import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { CustomersService } from './customers.service';

const URL = `${environment.apiBaseUrl}/api/v1/customers`;

describe('CustomersService', () => {
  let service: CustomersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CustomersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends a text search as the name filter', () => {
    service.list({ search: 'Ana', blacklist: 'all', page: 2 }).subscribe();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.params.get('name')).toBe('Ana');
    expect(req.request.params.has('phone')).toBe(false);
    expect(req.request.params.has('blacklisted')).toBe(false);
    expect(req.request.params.get('page')).toBe('2');
    req.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('sends a numeric search as the phone filter and the blacklist flag', () => {
    service.list({ search: '555 12', blacklist: 'blacklisted', page: 0 }).subscribe();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.params.get('phone')).toBe('555 12');
    expect(req.request.params.has('name')).toBe(false);
    expect(req.request.params.get('blacklisted')).toBe('true');
    req.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('posts the blacklist reason only when provided', () => {
    service.blacklist(7, 'No se presentó').subscribe();
    const withReason = http.expectOne(`${URL}/7/blacklist`);
    expect(withReason.request.method).toBe('POST');
    expect(withReason.request.body).toEqual({ reason: 'No se presentó' });
    withReason.flush(null);

    service.blacklist(7, null).subscribe();
    const withoutReason = http.expectOne(`${URL}/7/blacklist`);
    expect(withoutReason.request.body).toEqual({});
    withoutReason.flush(null);
  });

  it('removes a customer from the blacklist with DELETE', () => {
    service.removeFromBlacklist(7).subscribe();
    const req = http.expectOne(`${URL}/7/blacklist`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
