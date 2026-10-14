import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SKIP_ERROR_TOAST } from '@/core/interceptor/error.interceptor';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { ChatRequestDto, ChatResponseDto, SessionResponseDto } from '../models/chat.model';
import { AUDIO_EXTENSIONS } from '../models/voice.const';
import { TranscriptionResponseDto } from '../models/voice.model';

const AUDIO_PART = 'audio';
const AUDIO_FILE_BASE_NAME = 'voice';
const DEFAULT_AUDIO_EXTENSION = 'webm';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/agent`;
  // The chat renders its own "thinking" state, so the full-screen loader stays out.
  private readonly options = { context: new HttpContext().set(SKIP_LOADER, true) };
  // Dictation shows its errors inline under the composer instead of a toast.
  private readonly voiceOptions = { context: new HttpContext().set(SKIP_LOADER, true).set(SKIP_ERROR_TOAST, true) };

  createSession(): Observable<string> {
    return this.http.post<SessionResponseDto>(`${this.url}/session`, null, this.options).pipe(map((r) => r.sessionId));
  }

  chat(sessionId: string, message: string): Observable<string> {
    const body: ChatRequestDto = { sessionId, message };
    return this.http.post<ChatResponseDto>(`${this.url}/chat`, body, this.options).pipe(map((r) => r.reply));
  }

  /** Uploads a recording; Content-Type is left to the browser so it adds the multipart boundary. */
  transcribe(audio: Blob, mimeType: string): Observable<TranscriptionResponseDto> {
    const body = new FormData();
    body.append(AUDIO_PART, audio, `${AUDIO_FILE_BASE_NAME}.${audioExtension(mimeType)}`);
    return this.http.post<TranscriptionResponseDto>(`${this.url}/transcriptions`, body, this.voiceOptions);
  }
}

function audioExtension(mimeType: string): string {
  const baseType = mimeType.split(';')[0].trim().toLowerCase();
  return AUDIO_EXTENSIONS[baseType] ?? DEFAULT_AUDIO_EXTENSION;
}
