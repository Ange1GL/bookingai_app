import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AGENT_REPLY_TIMEOUT_MS, ChatResponseDto } from '../models/chat.model';
import { CreditsDto } from '../models/credits.model';
import { AgentService } from './agent.service';
import { ChatStore } from './chat.store';

const CREDITS: CreditsDto = {
  dailyLimit: 60000,
  used: 10000,
  remaining: 50000,
  percentUsed: 16,
  resetsAt: '2026-10-23T00:00:00-06:00',
};

function reply(text: string, balance: CreditsDto = CREDITS): ChatResponseDto {
  return { reply: text, credits: balance };
}

describe('ChatStore', () => {
  let createSession: ReturnType<typeof vi.fn<() => Observable<string>>>;
  let chat: ReturnType<typeof vi.fn<(sessionId: string, message: string) => Observable<ChatResponseDto>>>;
  let credits: ReturnType<typeof vi.fn<() => Observable<CreditsDto>>>;
  let store: ChatStore;

  beforeEach(() => {
    createSession = vi.fn(() => of('s-1'));
    chat = vi.fn(() => of(reply('Listo')));
    credits = vi.fn(() => of(CREDITS));
    TestBed.configureTestingModule({
      providers: [ChatStore, { provide: AgentService, useValue: { createSession, chat, credits } }],
    });
    store = TestBed.inject(ChatStore);
  });

  it('adds the user message and the reply', () => {
    store.send('  hola  ');

    expect(store.messages().map((m) => [m.role, m.text])).toEqual([
      ['user', 'hola'],
      ['assistant', 'Listo'],
    ]);
    expect(store.sending()).toBe(false);
  });

  it('creates the session only once', () => {
    store.send('uno');
    store.send('dos');

    expect(createSession).toHaveBeenCalledTimes(1);
    expect(chat).toHaveBeenNthCalledWith(2, 's-1', 'dos');
  });

  it('ignores blank text and sends while a request is in flight', () => {
    const pending = new Subject<ChatResponseDto>();
    chat.mockReturnValueOnce(pending);

    store.send('   ');
    store.send('primero');
    store.send('segundo');

    expect(store.messages().length).toBe(1);
    expect(store.sending()).toBe(true);
  });

  it('marks the message as failed and retries it', () => {
    chat.mockReturnValueOnce(throwError(() => new Error('boom')));
    store.send('agenda');

    const failed = store.messages()[0];
    expect(failed?.state).toBe('error');
    expect(store.sending()).toBe(false);

    store.retry(failed?.id ?? -1);

    expect(store.messages().map((m) => [m.role, m.state])).toEqual([
      ['user', 'sent'],
      ['assistant', 'sent'],
    ]);
  });

  it('treats a blank reply as no response so the manual fallback is offered', () => {
    chat.mockReturnValueOnce(of(reply('   ')));
    store.send('agenda');

    expect(store.messages().map((m) => [m.role, m.state])).toEqual([['user', 'error']]);
    expect(store.sending()).toBe(false);
  });

  it('gives up when the agent never answers', () => {
    vi.useFakeTimers();
    try {
      chat.mockReturnValueOnce(new Subject<ChatResponseDto>());
      store.send('agenda');

      vi.advanceTimersByTime(AGENT_REPLY_TIMEOUT_MS + 1);

      expect(store.messages()[0]?.state).toBe('error');
      expect(store.sending()).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('starts a new conversation with a fresh session', () => {
    store.send('uno');
    store.reset();
    expect(store.messages()).toEqual([]);

    store.send('dos');
    expect(createSession).toHaveBeenCalledTimes(2);
  });

  it('loads the credits when created', () => {
    expect(store.credits()).toEqual(CREDITS);
    expect(store.creditsLevel()).toBe('ok');
    expect(store.exhausted()).toBe(false);
  });

  it('keeps working when the credits cannot be loaded', () => {
    credits.mockReturnValueOnce(throwError(() => new Error('down')));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [ChatStore, { provide: AgentService, useValue: { createSession, chat, credits } }],
    });

    const fresh = TestBed.inject(ChatStore);

    expect(fresh.credits()).toBeNull();
    expect(fresh.exhausted()).toBe(false);
  });

  it('updates the credits with every reply', () => {
    const spent = { ...CREDITS, used: 55000, remaining: 5000, percentUsed: 91 };
    chat.mockReturnValueOnce(of(reply('Listo', spent)));

    store.send('agenda');

    expect(store.credits()).toEqual(spent);
    expect(store.creditsLevel()).toBe('low');
  });

  it('marks the message as blocked by quota, reloads the credits and offers no retry', () => {
    const empty = { ...CREDITS, used: 60000, remaining: 0, percentUsed: 100 };
    chat.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 429 })));
    credits.mockReturnValueOnce(of(empty));

    store.send('agenda');

    expect(store.messages()[0]?.state).toBe('quota');
    expect(store.exhausted()).toBe(true);
    store.retry(store.messages()[0]?.id ?? -1);
    expect(chat).toHaveBeenCalledTimes(1);
  });

  it('does not send while the credits are exhausted', () => {
    credits.mockReturnValueOnce(of({ ...CREDITS, used: 60000, remaining: 0, percentUsed: 100 }));
    store.loadCredits();

    store.send('agenda');

    expect(store.messages()).toEqual([]);
    expect(chat).not.toHaveBeenCalled();
  });
});
