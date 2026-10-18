import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, inject, Injectable } from '@angular/core';
import { ErrorHandlerService } from './error-handler.service';

/** Catches uncaught runtime errors; HTTP errors are already toasted by `errorInterceptor`. */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly errorHandler = inject(ErrorHandlerService);

  handleError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      return;
    }
    this.errorHandler.notifyUnexpected(error);
  }
}
