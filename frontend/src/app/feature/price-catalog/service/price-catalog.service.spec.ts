import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { PriceCatalogService } from './price-catalog.service';

const URL = `${environment.apiBaseUrl}/api/v1/price-catalog`;

describe('PriceCatalogService', () => {
  let service: PriceCatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PriceCatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the catalog', () => {
    service.list().subscribe();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('creates a service', () => {
    service.create({ label: 'Corte', price: 60 }).subscribe();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ label: 'Corte', price: 60 });
    req.flush({ id: 1, label: 'Corte', price: 60 });
  });

  it('updates a service by id', () => {
    service.update(3, { label: 'Corte + barba', price: 90 }).subscribe();

    const req = http.expectOne(`${URL}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ label: 'Corte + barba', price: 90 });
    req.flush({ id: 3, label: 'Corte + barba', price: 90 });
  });

  it('removes a service by id', () => {
    service.remove(3).subscribe();

    const req = http.expectOne(`${URL}/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
