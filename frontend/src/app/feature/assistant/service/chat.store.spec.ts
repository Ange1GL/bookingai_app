import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { AgentService } from './agent.service';
import { ChatStore } from './chat.store';

describe('ChatStore', () => {
  let createSession: ReturnType<typeof vi.fn<() => Observable<string>>>;
  let chat: ReturnType<typeof vi.fn<(sessionId: string, message: string) => Observable<string>>>;
  let store: ChatStore;

  beforeEach(() => {
    createSession = vi.fn(() => of('s-1'));
    chat = vi.fn(() => of('Listo'));
    TestBed.configureTestingModule({
      providers: [ChatStore, { provide: AgentService, useValue: { createSession, chat } }],
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
    const pending = new Subject<string>();
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

  it('starts a new conversation with a fresh session', () => {
    store.send('uno');
    store.reset();
    expect(store.messages()).toEqual([]);

    store.send('dos');
    expect(createSession).toHaveBeenCalledTimes(2);
  });
});
