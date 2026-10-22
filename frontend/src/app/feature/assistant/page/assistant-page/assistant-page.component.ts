import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NEW_APPOINTMENT_QUERY_PARAM } from '@/shared/navigation/appointment-links';
import { AssistantAvatarComponent } from '../../components/assistant-avatar/assistant-avatar.component';
import { ChatComposerComponent } from '../../components/chat-composer/chat-composer.component';
import { CreditsMeterComponent } from '../../components/credits-meter/credits-meter.component';
import { MessageBubbleComponent } from '../../components/message-bubble/message-bubble.component';
import { QuickActionsComponent } from '../../components/quick-actions/quick-actions.component';
import { SuggestionChipsComponent } from '../../components/suggestion-chips/suggestion-chips.component';
import { QuickAction } from '../../models/chat.model';
import { QUICK_ACTIONS, REPLY_CHIPS } from '../../models/quick-actions.const';
import { ChatStore } from '../../service/chat.store';

@Component({
  selector: 'app-assistant-page',
  imports: [
    DatePipe,
    AssistantAvatarComponent,
    CreditsMeterComponent,
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
  private readonly router = inject(Router);

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

  /** Opens the calendar with the new-appointment form ready, for when the agent cannot help. */
  protected createManually(): void {
    void this.router.navigate(['/appointments'], { queryParams: { [NEW_APPOINTMENT_QUERY_PARAM]: true } });
  }

  private scrollToEnd(): void {
    const list = this.messageList()?.nativeElement;
    if (list) {
      setTimeout(() => list.scrollTo({ top: list.scrollHeight, behavior: 'smooth' }));
    }
  }
}
