import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessionService } from '../service/session.service';
import { authGuard } from './auth.guard';

const REFRESH_URL = `${environment.apiBaseUrl}/api/v1/auth/refresh`;
const USER = { userId: 1, username: 'ana', email: 'ana@test.com', roles: [] };

describe('authGuard', () => {
  let http: HttpTestingController;

  const runGuard = () =>
    TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('allows navigation when a session exists', () => {
    TestBed.inject(SessionService).setUser(USER);
    expect(runGuard()).toBe(true);
  });

  it('restores the session from the refresh cookie', async () => {
    const result = runGuard();
    expect(isObservable(result)).toBe(true);
    const pending = firstValueFrom(result as Observable<boolean | UrlTree>);

    http.expectOne(REFRESH_URL).flush(USER);

    expect(await pending).toBe(true);
  });

  it('redirects to login when refresh fails', async () => {
    const pending = firstValueFrom(runGuard() as Observable<boolean | UrlTree>);

    http.expectOne(REFRESH_URL).flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });

    const result = await pending;
    expect(result.toString()).toBe(TestBed.inject(Router).parseUrl('/auth/login').toString());
  });
});
