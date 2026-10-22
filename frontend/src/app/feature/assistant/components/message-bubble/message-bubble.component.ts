import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ChatMessage } from '../../models/chat.model';
import { QUOTA_EXCEEDED_NOTE } from '../../models/credits.const';
import { AssistantAvatarComponent } from '../assistant-avatar/assistant-avatar.component';

@Component({
  selector: 'app-message-bubble',
  imports: [DatePipe, AssistantAvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './message-bubble.component.html',
})
export class MessageBubbleComponent {
  readonly message = input.required<ChatMessage>();
  readonly retry = output<number>();
  readonly createManually = output<void>();

  protected readonly quotaNote = QUOTA_EXCEEDED_NOTE;
  protected readonly isUser = computed(() => this.message().role === 'user');
  protected readonly bubbleClass = computed(() =>
    this.isUser()
      ? 'rounded-br-md bg-linear-to-br from-primary-500 to-primary-700 text-white shadow-md shadow-primary-500/20'
      : 'rounded-bl-md bg-surface-100 text-color dark:bg-surface-800',
  );
}
