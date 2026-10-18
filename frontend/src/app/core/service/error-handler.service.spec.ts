import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { ErrorHandlerService } from './error-handler.service';
import { GlobalErrorHandler } from './global-error-handler';

describe('ErrorHandlerService', () => {
  let service: ErrorHandlerService;
  let add: ReturnType<typeof vi.fn>;

  const httpError = (status: number, error: unknown = null) => new HttpErrorResponse({ status, error });

  beforeEach(() => {
    add = vi.fn();
    TestBed.configureTestingModule({ providers: [{ provide: MessageService, useValue: { add } }, GlobalErrorHandler] });
    service = TestBed.inject(ErrorHandlerService);
  });

  it('uses the backend message when present', () => {
    expect(service.extractMessage(httpError(409, { message: 'Ya existe' }))).toBe('Ya existe');
  });

  it('reports connectivity problems for status 0', () => {
    expect(service.extractMessage(httpError(0))).toBe('No se pudo conectar con el servidor');
  });

  it('falls back by status when the body is not the API error shape', () => {
    expect(service.extractMessage(httpError(429, '<html></html>'))).toContain('Demasiadas solicitudes');
    expect(service.extractMessage(httpError(503, { message: '  ' }))).toContain('servidor');
    expect(service.extractMessage(httpError(418))).toBe('Ocurrió un error inesperado');
  });

  it('uses warn for client errors and error for server errors', () => {
    service.notify(httpError(400, { message: 'mal' }));
    service.notify(httpError(502, { message: 'caído' }));

    expect(add.mock.calls[0][0].severity).toBe('warn');
    expect(add.mock.calls[1][0].severity).toBe('error');
  });

  it('does not stack identical toasts fired together', () => {
    service.notify(httpError(0));
    service.notify(httpError(0));

    expect(add).toHaveBeenCalledTimes(1);
  });

  it('GlobalErrorHandler toasts runtime errors but ignores HTTP errors', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const handler = TestBed.inject(GlobalErrorHandler);

    handler.handleError(httpError(500));
    expect(add).not.toHaveBeenCalled();

    handler.handleError(new Error('boom'));
    expect(add).toHaveBeenCalledTimes(1);
  });
});
