import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { ErrorHandlerService } from '../service/error-handler.service';

/** Shows a toast for errors that were not recovered by an inner interceptor. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorHandler = inject(ErrorHandlerService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // A failed silent refresh just means "not logged in"; the guard redirects to login.
      if (req.url !== API_ENDPOINTS.auth.refresh) {
        errorHandler.notify(error);
      }
      return throwError(() => error);
    }),
  );
};
