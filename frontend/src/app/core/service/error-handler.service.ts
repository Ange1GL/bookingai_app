import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ApiErrorDto } from '../model/api-error.dto';

const NETWORK_ERROR_STATUS = 0;
const NETWORK_ERROR_MESSAGE = 'No se pudo conectar con el servidor';
const UNKNOWN_ERROR_MESSAGE = 'Ocurrió un error inesperado';

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  private readonly messageService = inject(MessageService);

  extractMessage(error: HttpErrorResponse): string {
    if (error.status === NETWORK_ERROR_STATUS) {
      return NETWORK_ERROR_MESSAGE;
    }
    const body = error.error as Partial<ApiErrorDto> | null;
    return body?.message ?? UNKNOWN_ERROR_MESSAGE;
  }

  notify(error: HttpErrorResponse): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: this.extractMessage(error),
    });
  }
}
