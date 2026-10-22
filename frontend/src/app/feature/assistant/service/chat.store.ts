import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, EMPTY, map, Observable, of, switchMap, tap, timeout } from 'rxjs';
import { AGENT_REPLY_TIMEOUT_MS, ChatMessage, ChatResponseDto, ChatRole } from '../models/chat.model';
import { QUOTA_EXCEEDED_STATUS } from '../models/credits.const';
import { CreditsDto } from '../models/credits.model';
import { creditsLevel } from '../models/credits.util';
import { AgentService } from './agent.service';

/** Conversation state for the assistant page; provided at route level so it lives with the page. */
@Injectable()
export class ChatStore {
  private readonly agent = inject(AgentService);

  private readonly messagesState = signal<ChatMessage[]>([]);
  private readonly sendingState = signal(false);
  private readonly creditsState = signal<CreditsDto | null>(null);
  private sessionId: string | null = null;
  private nextId = 1;

  readonly messages = this.messagesState.asReadonly();
  readonly sending = this.sendingState.asReadonly();
  /** `null` until the first load; the page works without it (the backend still enforces the limit). */
  readonly credits = this.creditsState.asReadonly();
  readonly creditsLevel = computed(() => {
    const credits = this.creditsState();
    return credits ? creditsLevel(credits) : 'ok';
  });
  readonly exhausted = computed(() => this.creditsLevel() === 'exhausted');

  constructor() {
    this.loadCredits();
  }

  /** Refreshes the balance; a failure keeps the last known value. */
  loadCredits(): void {
    this.agent
      .credits()
      .pipe(catchError(() => EMPTY))
      .subscribe((credits) => this.creditsState.set(credits));
  }

  send(text: string): void {
    const message = text.trim();
    if (!message || this.sendingState() || this.exhausted()) {
      return;
    }
    const id = this.append('user', message);
    this.deliver(id, message);
  }

  /** Re-sends a user message whose delivery failed. */
  retry(id: number): void {
    const failed = this.messagesState().find((m) => m.id === id && m.state === 'error');
    if (!failed || this.sendingState() || this.exhausted()) {
      return;
    }
    this.updateState(id, 'sent');
    this.deliver(id, failed.text);
  }

  /** Starts a fresh conversation: clears the history and drops the server-side session. */
  reset(): void {
    if (this.sendingState()) {
      return;
    }
    this.messagesState.set([]);
    this.sessionId = null;
  }

  private deliver(id: number, message: string): void {
    this.sendingState.set(true);
    this.ensureSession()
      .pipe(
        switchMap((sessionId) => this.agent.chat(sessionId, message)),
        timeout({ first: AGENT_REPLY_TIMEOUT_MS }),
        map((response) => this.validated(response)),
      )
      .subscribe({
        next: (response) => {
          this.creditsState.set(response.credits);
          this.append('assistant', response.reply);
          this.sendingState.set(false);
        },
        error: (error: unknown) => {
          if (isQuotaExceeded(error)) {
            this.updateState(id, 'quota');
            this.loadCredits();
          } else {
            this.updateState(id, 'error');
          }
          this.sendingState.set(false);
        },
      });
  }

  private validated(response: ChatResponseDto): ChatResponseDto {
    if (!response.reply?.trim()) {
      throw new Error('Empty agent reply');
    }
    return response;
  }

  private ensureSession(): Observable<string> {
    if (this.sessionId) {
      return of(this.sessionId);
    }
    return this.agent.createSession().pipe(tap((sessionId) => (this.sessionId = sessionId)));
  }

  private append(role: ChatRole, text: string): number {
    const id = this.nextId++;
    this.messagesState.update((messages) => [...messages, { id, role, text, state: 'sent', createdAt: new Date() }]);
    return id;
  }

  private updateState(id: number, state: ChatMessage['state']): void {
    this.messagesState.update((messages) => messages.map((m) => (m.id === id ? { ...m, state } : m)));
  }
}

function isQuotaExceeded(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === QUOTA_EXCEEDED_STATUS;
}
