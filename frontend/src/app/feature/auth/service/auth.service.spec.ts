import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { AuthUserDto } from '@/core/model/auth-user.dto';
import { SessionService } from '@/core/service/session.service';
import { AuthService } from './auth.service';

const USER: AuthUserDto = { userId: 1, username: 'ana', email: 'ana@test.com', roles: ['USER'] };

describe('AuthService', () => {
  let service: AuthService;
  let session: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    session = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the user in the session after login', () => {
    service.login({ username: 'ana', password: 'secret' }).subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(USER);

    expect(session.user()).toEqual(USER);
    expect(session.isAuthenticated()).toBe(true);
  });

  it('stores the user in the session after register', () => {
    service.register({ username: 'ana', email: 'ana@test.com', password: 'secret123', name: 'Ana' }).subscribe();

    http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/register`).flush(USER);

    expect(session.user()).toEqual(USER);
  });
});
