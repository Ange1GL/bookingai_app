import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Observable, shareReplay, tap } from 'rxjs';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { LOGIN_URL } from '../constants/routes';
import { AuthUserDto } from '../model/auth-user.dto';

/**
 * Holds the authenticated user. Tokens live in HttpOnly cookies managed by the backend,
 * so the client only keeps the user profile returned by login/register/refresh.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly userState = signal<AuthUserDto | null>(null);
  private refreshInFlight$: Observable<AuthUserDto> | null = null;

  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => this.userState() !== null);

  setUser(user: AuthUserDto): void {
    this.userState.set(user);
  }

  /** Shares a single refresh call between concurrent callers (guard, interceptor). */
  refresh(): Observable<AuthUserDto> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<AuthUserDto>(API_ENDPOINTS.auth.refresh, null).pipe(
        tap((user) => this.setUser(user)),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshInFlight$;
  }

  logout(): Observable<void> {
    return this.http.post<void>(API_ENDPOINTS.auth.logout, null).pipe(finalize(() => this.expire()));
  }

  /** Clears local state and sends the user to the login page. */
  expire(): void {
    this.userState.set(null);
    void this.router.navigateByUrl(LOGIN_URL);
  }
}
