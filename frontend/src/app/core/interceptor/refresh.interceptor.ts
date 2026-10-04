import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { SessionService } from '../service/session.service';

const AUTH_PATH = '/api/v1/auth/';

/** On 401, refreshes the session once and retries the original request. */
export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isRecoverable = error.status === HttpStatusCode.Unauthorized && !req.url.includes(AUTH_PATH);
      if (!isRecoverable) {
        return throwError(() => error);
      }
      return session.refresh().pipe(
        switchMap(() => next(req)),
        catchError((refreshError: HttpErrorResponse) => {
          session.expire();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
