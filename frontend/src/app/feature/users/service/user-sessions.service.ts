import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { UserSessionDto } from '../models/user-session.dto';

// Every caller renders its own loading state (skeletons, button spinners).
const ownLoading = () => new HttpContext().set(SKIP_LOADER, true);

@Injectable({ providedIn: 'root' })
export class UserSessionsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/users`;

  /** Active sessions of the user, most recent first. */
  list(userId: number): Observable<UserSessionDto[]> {
    return this.http.get<UserSessionDto[]>(`${this.url}/${userId}/sessions`, { context: ownLoading() });
  }

  /** Closes one session. Its access token can keep working until it expires (up to 15 min). */
  revoke(userId: number, sessionId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${userId}/sessions/${sessionId}`, { context: ownLoading() });
  }

  /** Closes every session of the user; their access tokens stop working immediately. */
  revokeAll(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${userId}/invalid-session`, { context: ownLoading() });
  }
}
