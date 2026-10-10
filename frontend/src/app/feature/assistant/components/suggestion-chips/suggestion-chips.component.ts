import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { QuickAction } from '../../models/chat.model';

/** Horizontally scrollable quick replies shown above the composer. */
@Component({
  selector: 'app-suggestion-chips',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './suggestion-chips.component.html',
})
export class SuggestionChipsComponent {
  readonly suggestions = input.required<QuickAction[]>();
  readonly disabled = input(false);
  readonly chosen = output<QuickAction>();
}
