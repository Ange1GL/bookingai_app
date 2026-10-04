import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { SessionService } from '../service/session.service';
import { refreshInterceptor } from './refresh.interceptor';

const DATA_URL = `${environment.apiBaseUrl}/api/v1/appointments`;
const REFRESH_URL = `${environment.apiBaseUrl}/api/v1/auth/refresh`;
const LOGIN_URL = `${environment.apiBaseUrl}/api/v1/auth/login`;
const USER = { userId: 1, username: 'ana', email: 'ana@test.com', roles: [] };
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };

describe('refreshInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let session: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([refreshInterceptor])), provideHttpClientTesting(), provideRouter([])],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
  });

  afterEach(() => http.verify());

  it('refreshes once and retries the original request on 401', () => {
    let body: unknown;
    client.get(DATA_URL).subscribe((value) => (body = value));

    http.expectOne(DATA_URL).flush(null, UNAUTHORIZED);
    http.expectOne(REFRESH_URL).flush(USER);
    http.expectOne(DATA_URL).flush({ ok: true });

    expect(body).toEqual({ ok: true });
    expect(session.user()).toEqual(USER);
  });

  it('expires the session when the refresh fails', () => {
    const expire = vi.spyOn(session, 'expire').mockImplementation(() => undefined);
    let failed = false;
    client.get(DATA_URL).subscribe({ error: () => (failed = true) });

    http.expectOne(DATA_URL).flush(null, UNAUTHORIZED);
    http.expectOne(REFRESH_URL).flush(null, UNAUTHORIZED);

    expect(failed).toBe(true);
    expect(expire).toHaveBeenCalledOnce();
  });

  it('does not try to refresh when an auth endpoint returns 401', () => {
    let failed = false;
    client.post(LOGIN_URL, {}).subscribe({ error: () => (failed = true) });

    http.expectOne(LOGIN_URL).flush(null, UNAUTHORIZED);

    expect(failed).toBe(true);
  });
});
