import { HttpClient, HttpXsrfTokenExtractor, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { csrfInterceptor } from './csrf.interceptor';

const API = environment.apiBaseUrl;
const SEED_URL = `${API}/api/v1/auth/csrf`;
const HEADER = 'X-XSRF-TOKEN';

describe('csrfInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let token: string | null;

  beforeEach(() => {
    token = null;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([csrfInterceptor])),
        provideHttpClientTesting(),
        { provide: HttpXsrfTokenExtractor, useValue: { getToken: () => token } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds the header to cross-origin API mutations when the cookie exists', () => {
    token = 'abc';
    http.post(`${API}/api/v1/x`, {}).subscribe();

    expect(controller.expectOne(`${API}/api/v1/x`).request.headers.get(HEADER)).toBe('abc');
  });

  it('seeds the cookie with a GET before the first mutation', () => {
    http.post(`${API}/api/v1/x`, {}).subscribe();

    token = 'seeded'; // the seed response makes the browser store the cookie
    controller.expectOne(SEED_URL).flush('');
    expect(controller.expectOne(`${API}/api/v1/x`).request.headers.get(HEADER)).toBe('seeded');
  });

  it('does not touch safe methods', () => {
    http.get(`${API}/api/v1/x`).subscribe();

    expect(controller.expectOne(`${API}/api/v1/x`).request.headers.has(HEADER)).toBe(false);
  });

  it('does not touch requests to other hosts', () => {
    token = 'abc';
    http.post('https://other.example.com/x', {}).subscribe();

    expect(controller.expectOne('https://other.example.com/x').request.headers.has(HEADER)).toBe(false);
  });
});
