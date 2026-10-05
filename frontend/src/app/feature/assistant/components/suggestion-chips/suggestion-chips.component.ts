import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { QuickAction } from '../../models/chat.model';

/** Horizontally scrollable quick replies shown above the composer. */
@Component({
  selector: 'app-suggestion-chips',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="m-0 flex list-none gap-2 overflow-x-auto p-0 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      @for (suggestion of suggestions(); track suggestion.label) {
        <li class="shrink-0">
          <button
            type="button"
            class="flex min-h-10 cursor-pointer items-center gap-1.5 rounded-full border border-surface bg-surface-0 px-3.5 text-sm font-medium text-primary transition hover:bg-primary-50 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-surface-900 dark:hover:bg-primary-500/10"
            [disabled]="disabled()"
            (click)="chosen.emit(suggestion)"
          >
            <i class="pi text-xs" [class]="suggestion.icon" aria-hidden="true"></i>
            {{ suggestion.label }}
          </button>
        </li>
      }
    </ul>
  `,
})
export class SuggestionChipsComponent {
  readonly suggestions = input.required<QuickAction[]>();
  readonly disabled = input(false);
  readonly chosen = output<QuickAction>();
}
