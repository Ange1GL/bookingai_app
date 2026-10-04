import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SessionService } from '../service/session.service';

/** Allows navigation when a session exists, restoring it from the refresh cookie after a reload. */
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return true;
  }
  return session.refresh().pipe(
    map(() => true),
    catchError(() => of(router.parseUrl('/auth/login'))),
  );
};
