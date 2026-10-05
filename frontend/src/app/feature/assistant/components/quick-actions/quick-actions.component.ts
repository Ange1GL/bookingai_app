import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { QuickAction } from '../../models/chat.model';

/** 2×2 cards shown on the welcome screen. */
@Component({
  selector: 'app-quick-actions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="m-0 grid w-full list-none grid-cols-2 gap-3 p-0">
      @for (action of actions(); track action.label) {
        <li>
          <button
            type="button"
            class="flex h-full w-full cursor-pointer flex-col items-start gap-1.5 rounded-2xl border border-surface bg-surface-0 p-3 text-left transition hover:border-primary-300 hover:shadow-md active:scale-[0.98] dark:bg-surface-900"
            (click)="chosen.emit(action)"
          >
            <span
              class="flex size-9 items-center justify-center rounded-xl bg-primary-50 text-primary dark:bg-primary-500/10"
              aria-hidden="true"
            >
              <i class="pi" [class]="action.icon"></i>
            </span>
            <span class="text-sm font-semibold">{{ action.label }}</span>
            <span class="text-xs leading-snug text-muted-color">{{ action.description }}</span>
          </button>
        </li>
      }
    </ul>
  `,
})
export class QuickActionsComponent {
  readonly actions = input.required<QuickAction[]>();
  readonly chosen = output<QuickAction>();
}
