import { HttpClient, HttpInterceptorFn, HttpXsrfTokenExtractor } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';

const CSRF_HEADER = 'X-XSRF-TOKEN';
const CSRF_SEED_URL = `${environment.apiBaseUrl}/api/v1/auth/csrf`;
const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS'];

/**
 * Double-submit cookie for the cross-origin API. Angular's built-in XSRF interceptor skips
 * requests whose origin differs from the app's, so the header is added here instead.
 * When the XSRF-TOKEN cookie does not exist yet, a GET to the backend seeds it first.
 */
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const isApiMutation = req.url.startsWith(environment.apiBaseUrl) && !SAFE_METHODS.includes(req.method);
  if (!isApiMutation) {
    return next(req);
  }

  const tokenExtractor = inject(HttpXsrfTokenExtractor);
  const sendWithToken = () => {
    const token = tokenExtractor.getToken();
    return next(token ? req.clone({ setHeaders: { [CSRF_HEADER]: token } }) : req);
  };

  if (tokenExtractor.getToken()) {
    return sendWithToken();
  }
  return inject(HttpClient)
    .get(CSRF_SEED_URL, { responseType: 'text' })
    .pipe(switchMap(sendWithToken));
};
