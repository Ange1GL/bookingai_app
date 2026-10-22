import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

/** Borderless icon-only button. The tooltip says what it does and doubles as the accessible name. */
@Component({
  selector: 'app-icon-button',
  imports: [ButtonModule, TooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './icon-button.component.html',
})
export class IconButtonComponent {
  readonly icon = input.required<string>();
  /** Short description shown on hover/focus and read by screen readers. */
  readonly tooltip = input.required<string>();
  readonly severity = input<'primary' | 'secondary' | 'danger'>('primary');
  readonly disabled = input(false);
  readonly clicked = output<void>();
}
