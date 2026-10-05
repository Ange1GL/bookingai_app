import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, viewChild } from '@angular/core';
import { AssistantAvatarComponent } from '../../components/assistant-avatar/assistant-avatar.component';
import { ChatComposerComponent } from '../../components/chat-composer/chat-composer.component';
import { MessageBubbleComponent } from '../../components/message-bubble/message-bubble.component';
import { QuickActionsComponent } from '../../components/quick-actions/quick-actions.component';
import { SuggestionChipsComponent } from '../../components/suggestion-chips/suggestion-chips.component';
import { QuickAction } from '../../models/chat.model';
import { QUICK_ACTIONS, REPLY_CHIPS } from '../../models/quick-actions.const';
import { ChatStore } from '../../service/chat.store';

@Component({
  selector: 'app-assistant-page',
  imports: [
    AssistantAvatarComponent,
    MessageBubbleComponent,
    ChatComposerComponent,
    QuickActionsComponent,
    SuggestionChipsComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './assistant-page.component.html',
})
export class AssistantPageComponent {
  protected readonly store = inject(ChatStore);

  private readonly messageList = viewChild<ElementRef<HTMLElement>>('messageList');

  protected readonly quickActions = QUICK_ACTIONS;
  protected readonly replyChips = REPLY_CHIPS;

  constructor() {
    effect(() => {
      this.store.messages();
      this.store.sending();
      this.scrollToEnd();
    });
  }

  protected run(action: QuickAction): void {
    this.store.send(action.prompt);
  }

  private scrollToEnd(): void {
    const list = this.messageList()?.nativeElement;
    if (list) {
      setTimeout(() => list.scrollTo({ top: list.scrollHeight, behavior: 'smooth' }));
    }
  }
}
