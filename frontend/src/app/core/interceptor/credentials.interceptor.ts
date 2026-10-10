import { HttpInterceptorFn } from '@angular/common/http';
import { isApiUrl } from '../util/api-url';

/** Sends the HttpOnly auth cookies with every request to the API. */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) =>
  isApiUrl(req.url) ? next(req.clone({ withCredentials: true })) : next(req);
