import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { PasswordRecoveryRequestDto } from '../models/password-recovery-request.dto';

/** Admin inbox of users who asked to recover their password. They are resolved with `UsersService.resetPassword`. */
@Injectable({ providedIn: 'root' })
export class PasswordRecoveryRequestsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/password-recovery-requests`;

  listPending(): Observable<PasswordRecoveryRequestDto[]> {
    return this.http.get<PasswordRecoveryRequestDto[]>(this.url, { context: new HttpContext().set(SKIP_LOADER, true) });
  }
}
