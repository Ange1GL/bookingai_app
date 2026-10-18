import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { SKIP_ERROR_TOAST } from '@/core/interceptor/error.interceptor';
import { SKIP_LOADER } from '@/core/interceptor/loader.interceptor';
import { AgentService } from './agent.service';

const BASE = `${environment.apiBaseUrl}/api/v1/agent`;

describe('AgentService', () => {
  let http: HttpTestingController;
  let service: AgentService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(AgentService);
  });

  afterEach(() => http.verify());

  it('creates a session and returns its id', () => {
    let id = '';
    service.createSession().subscribe((value) => (id = value));
    http.expectOne(`${BASE}/session`).flush({ sessionId: 's-1' });
    expect(id).toBe('s-1');
  });

  it('posts the message with its session and returns the reply', () => {
    let reply = '';
    service.chat('s-1', 'hola').subscribe((value) => (reply = value));
    const req = http.expectOne(`${BASE}/chat`);
    expect(req.request.body).toEqual({ sessionId: 's-1', message: 'hola' });
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBe(true);
    req.flush({ reply: 'Hola, ¿en qué te ayudo?' });
    expect(reply).toBe('Hola, ¿en qué te ayudo?');
  });

  it('uploads the recording as multipart with an extension that matches its type', () => {
    const audio = new Blob(['audio'], { type: 'audio/mp4' });
    let text = '';
    service.transcribe(audio, 'audio/mp4').subscribe((response) => (text = response.text));

    const req = http.expectOne(`${BASE}/transcriptions`);
    const body = req.request.body as FormData;
    const part = body.get('audio') as File;
    expect(req.request.method).toBe('POST');
    expect(part.name).toBe('voice.mp4');
    expect(req.request.headers.has('Content-Type')).toBe(false);
    expect(req.request.context.get(SKIP_LOADER)).toBe(true);
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBe(true);
    req.flush({ text: 'Agenda a Juan', remainingToday: 39 });
    expect(text).toBe('Agenda a Juan');
  });

  it('ignores codec parameters when naming the audio file', () => {
    service.transcribe(new Blob(['a']), 'audio/webm;codecs=opus').subscribe();

    const req = http.expectOne(`${BASE}/transcriptions`);
    expect(((req.request.body as FormData).get('audio') as File).name).toBe('voice.webm');
    req.flush({ text: 'hola', remainingToday: 1 });
  });
});
