import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
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
    req.flush({ reply: 'Hola, ¿en qué te ayudo?' });
    expect(reply).toBe('Hola, ¿en qué te ayudo?');
  });
});
