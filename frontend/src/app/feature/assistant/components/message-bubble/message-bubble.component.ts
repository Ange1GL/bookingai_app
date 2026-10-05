import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ChatMessage } from '../../models/chat.model';
import { AssistantAvatarComponent } from '../assistant-avatar/assistant-avatar.component';

@Component({
  selector: 'app-message-bubble',
  imports: [DatePipe, AssistantAvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-end gap-2" [class]="isUser() ? 'flex-row-reverse' : 'flex-row'">
      @if (!isUser()) {
        <app-assistant-avatar />
      }
      <div class="flex min-w-0 max-w-[82%] flex-col gap-1" [class]="isUser() ? 'items-end' : 'items-start'">
        <p
          class="m-0 whitespace-pre-wrap wrap-break-word rounded-2xl px-4 py-2.5 text-[0.95rem] leading-relaxed"
          [class]="bubbleClass()"
        >
          {{ message().text }}
        </p>
        <span class="flex items-center gap-1 px-1 text-[11px] text-muted-color">
          {{ message().createdAt | date: 'shortTime' }}
          @if (isUser() && message().state === 'sent') {
            <i class="pi pi-check text-[9px]" aria-label="Enviado"></i>
          }
        </span>
        @if (message().state === 'error') {
          <button
            type="button"
            class="flex min-h-9 cursor-pointer items-center gap-1.5 rounded-full bg-rose-50 px-3 text-xs font-medium text-rose-600 dark:bg-rose-500/10 dark:text-rose-400"
            (click)="retry.emit(message().id)"
          >
            <i class="pi pi-refresh text-[10px]" aria-hidden="true"></i>
            No se pudo enviar · Reintentar
          </button>
        }
      </div>
    </div>
  `,
})
export class MessageBubbleComponent {
  readonly message = input.required<ChatMessage>();
  readonly retry = output<number>();

  protected readonly isUser = computed(() => this.message().role === 'user');
  protected readonly bubbleClass = computed(() =>
    this.isUser()
      ? 'rounded-br-md bg-linear-to-br from-primary-500 to-primary-700 text-white shadow-md shadow-primary-500/20'
      : 'rounded-bl-md bg-surface-100 text-color dark:bg-surface-800',
  );
}
