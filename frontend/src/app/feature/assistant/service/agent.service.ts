import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { ChatRequestDto, ChatResponseDto, SessionResponseDto } from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/agent`;
  // The chat renders its own "thinking" state, so the full-screen loader stays out.
  private readonly options = { context: new HttpContext().set(SKIP_LOADER, true) };

  createSession(): Observable<string> {
    return this.http.post<SessionResponseDto>(`${this.url}/session`, null, this.options).pipe(map((r) => r.sessionId));
  }

  chat(sessionId: string, message: string): Observable<string> {
    const body: ChatRequestDto = { sessionId, message };
    return this.http.post<ChatResponseDto>(`${this.url}/chat`, body, this.options).pipe(map((r) => r.reply));
  }
}
