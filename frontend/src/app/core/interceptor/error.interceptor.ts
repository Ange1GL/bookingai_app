import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ErrorHandlerService } from '../service/error-handler.service';

/** Set to `true` in the request context for calls whose caller shows the error inline instead of a toast. */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/** Shows a toast for errors that were not recovered by an inner interceptor. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorHandler = inject(ErrorHandlerService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // A failed silent refresh just means "not logged in"; the guard redirects to login.
      if (!req.url.endsWith('/api/v1/auth/refresh') && !req.context.get(SKIP_ERROR_TOAST)) {
        errorHandler.notify(error);
      }
      return throwError(() => error);
    }),
  );
};
