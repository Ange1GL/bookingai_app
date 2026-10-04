import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ErrorHandlerService } from '../service/error-handler.service';

/** Shows a toast for errors that were not recovered by an inner interceptor. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorHandler = inject(ErrorHandlerService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // A failed silent refresh just means "not logged in"; the guard redirects to login.
      if (!req.url.endsWith('/api/v1/auth/refresh')) {
        errorHandler.notify(error);
      }
      return throwError(() => error);
    }),
  );
};
