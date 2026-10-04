import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/** Sends the HttpOnly auth cookies with every request to the API. */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) =>
  req.url.startsWith(environment.apiBaseUrl) ? next(req.clone({ withCredentials: true })) : next(req);
