import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ApiErrorDto } from '../model/api-error.dto';

const NETWORK_ERROR_STATUS = 0;
const NETWORK_ERROR_MESSAGE = 'No se pudo conectar con el servidor';
const UNKNOWN_ERROR_MESSAGE = 'Ocurrió un error inesperado';
const SERVER_ERROR_MESSAGE = 'El servidor no pudo procesar la solicitud. Inténtalo más tarde';
const DUPLICATE_WINDOW_MS = 2000;

/** Used only when the backend body carries no usable `message` (proxy/CloudFront pages, Spring defaults). */
const FALLBACK_MESSAGES: Partial<Record<number, string>> = {
  [HttpStatusCode.BadRequest]: 'La solicitud no es válida',
  [HttpStatusCode.Unauthorized]: 'Tu sesión no es válida. Inicia sesión de nuevo',
  [HttpStatusCode.Forbidden]: 'No tienes permiso para realizar esta acción',
  [HttpStatusCode.NotFound]: 'No se encontró el recurso solicitado',
  [HttpStatusCode.Conflict]: 'La acción entra en conflicto con el estado actual',
  [HttpStatusCode.PayloadTooLarge]: 'El archivo supera el tamaño máximo permitido',
  [HttpStatusCode.TooManyRequests]: 'Demasiadas solicitudes. Espera un momento e inténtalo de nuevo',
};

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  private readonly messageService = inject(MessageService);
  private lastToast = { detail: '', at: 0 };

  extractMessage(error: HttpErrorResponse): string {
    if (error.status === NETWORK_ERROR_STATUS) {
      return NETWORK_ERROR_MESSAGE;
    }
    return this.bodyMessage(error) ?? this.fallbackMessage(error.status);
  }

  notify(error: HttpErrorResponse): void {
    const isServerSide = error.status === NETWORK_ERROR_STATUS || error.status >= HttpStatusCode.InternalServerError;
    this.show(isServerSide ? 'error' : 'warn', this.extractMessage(error));
  }

  /** For non-HTTP failures (runtime errors); the technical detail goes to the console, never to the user. */
  notifyUnexpected(error: unknown): void {
    console.error(error);
    this.show('error', UNKNOWN_ERROR_MESSAGE);
  }

  private bodyMessage(error: HttpErrorResponse): string | null {
    const body = error.error as Partial<ApiErrorDto> | string | null;
    const message = typeof body === 'object' && body !== null ? body.message : undefined;
    return typeof message === 'string' && message.trim() ? message : null;
  }

  private fallbackMessage(status: number): string {
    if (status >= HttpStatusCode.InternalServerError) {
      return SERVER_ERROR_MESSAGE;
    }
    return FALLBACK_MESSAGES[status] ?? UNKNOWN_ERROR_MESSAGE;
  }

  /** Parallel requests failing together (e.g. offline) would stack identical toasts. */
  private show(severity: 'error' | 'warn', detail: string): void {
    const now = Date.now();
    if (detail === this.lastToast.detail && now - this.lastToast.at < DUPLICATE_WINDOW_MS) {
      return;
    }
    this.lastToast = { detail, at: now };
    this.messageService.add({ severity, summary: severity === 'error' ? 'Error' : 'Aviso', detail });
  }
}
