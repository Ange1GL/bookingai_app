import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { QuickAction } from '../../models/chat.model';

/** 2×2 cards shown on the welcome screen. */
@Component({
  selector: 'app-quick-actions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quick-actions.component.html',
})
export class QuickActionsComponent {
  readonly actions = input.required<QuickAction[]>();
  readonly chosen = output<QuickAction>();
}
