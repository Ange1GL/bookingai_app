import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { AuthUserDto } from '@/core/model/auth-user.dto';
import { SessionService } from '@/core/service/session.service';
import { AuthService } from './auth.service';

const USER: AuthUserDto = { userId: 1, username: 'ana', email: 'ana@test.com', roles: ['USER'], permissions: [] };

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

  it('verifies the code without touching the session and returns the ticket', () => {
    let token = '';
    service.verifyCode({ username: 'ana', code: '483920' }).subscribe((response) => (token = response.verificationToken));

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/verify-code`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'ana', code: '483920' });
    req.flush({ verificationToken: 'ticket', expiresAt: '2026-10-20T12:10:00Z' });

    expect(token).toBe('ticket');
    expect(session.user()).toBeNull();
  });

  it('sends the one-time code and stores the user after creating the password', () => {
    service.createPassword({ username: 'ana', code: '483920', verificationToken: 'ticket', newPassword: 'Mi-clave-2026' }).subscribe();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/create-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      username: 'ana',
      code: '483920',
      verificationToken: 'ticket',
      newPassword: 'Mi-clave-2026',
    });
    req.flush(USER);

    expect(session.user()).toEqual(USER);
    expect(session.isAuthenticated()).toBe(true);
  });

  it('does not store a user when the code is rejected', () => {
    service.createPassword({ username: 'ana', code: '000000', verificationToken: 'ticket', newPassword: 'Mi-clave-2026' }).subscribe({
      error: () => undefined,
    });

    http
      .expectOne(`${environment.apiBaseUrl}/api/v1/auth/create-password`)
      .flush({ message: 'Código inválido o vencido' }, { status: 400, statusText: 'Bad Request' });

    expect(session.user()).toBeNull();
  });

  it('stores the user in the session after register', () => {
    service.register({ username: 'ana', email: 'ana@test.com', password: 'secret123', name: 'Ana' }).subscribe();

    http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/register`).flush(USER);

    expect(session.user()).toEqual(USER);
  });
});
