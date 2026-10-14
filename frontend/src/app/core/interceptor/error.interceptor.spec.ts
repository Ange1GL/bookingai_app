import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErrorHandlerService } from '../service/error-handler.service';
import { errorInterceptor, SKIP_ERROR_TOAST } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let notify: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    notify = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorHandlerService, useValue: { notify } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function failRequest(context?: HttpContext): void {
    http.get('/api/v1/anything', { context }).subscribe({ error: () => undefined });
    controller.expectOne('/api/v1/anything').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
  }

  it('shows a toast for unhandled errors', () => {
    failRequest();

    expect(notify).toHaveBeenCalledTimes(1);
  });

  it('skips the toast when the caller shows the error inline', () => {
    failRequest(new HttpContext().set(SKIP_ERROR_TOAST, true));

    expect(notify).not.toHaveBeenCalled();
  });
});
