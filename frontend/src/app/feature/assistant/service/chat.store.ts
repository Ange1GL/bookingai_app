import { inject, Injectable, signal } from '@angular/core';
import { Observable, of, switchMap, tap } from 'rxjs';
import { ChatMessage, ChatRole } from '../models/chat.model';
import { AgentService } from './agent.service';

/** Conversation state for the assistant page; provided at route level so it lives with the page. */
@Injectable()
export class ChatStore {
  private readonly agent = inject(AgentService);

  private readonly messagesState = signal<ChatMessage[]>([]);
  private readonly sendingState = signal(false);
  private sessionId: string | null = null;
  private nextId = 1;

  readonly messages = this.messagesState.asReadonly();
  readonly sending = this.sendingState.asReadonly();

  send(text: string): void {
    const message = text.trim();
    if (!message || this.sendingState()) {
      return;
    }
    const id = this.append('user', message);
    this.deliver(id, message);
  }

  /** Re-sends a user message whose delivery failed. */
  retry(id: number): void {
    const failed = this.messagesState().find((m) => m.id === id && m.state === 'error');
    if (!failed || this.sendingState()) {
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
      .pipe(switchMap((sessionId) => this.agent.chat(sessionId, message)))
      .subscribe({
        next: (reply) => {
          this.append('assistant', reply);
          this.sendingState.set(false);
        },
        error: () => {
          this.updateState(id, 'error');
          this.sendingState.set(false);
        },
      });
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
