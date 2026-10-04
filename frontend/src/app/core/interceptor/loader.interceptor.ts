import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoaderService } from '../service/loader.service';

/** Set to `true` in the request context for calls whose caller renders its own loading state. */
export const SKIP_LOADER = new HttpContextToken<boolean>(() => false);

export const loaderInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(SKIP_LOADER)) {
    return next(req);
  }
  const loader = inject(LoaderService);
  loader.show();
  return next(req).pipe(finalize(() => loader.hide()));
};
