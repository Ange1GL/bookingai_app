import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

export type ConfirmSeverity = 'primary' | 'danger';

/** Modal asking for confirmation. Extra fields (e.g. a reason) can be projected as content. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [DialogModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  readonly visible = model(false);
  readonly header = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Confirmar');
  readonly cancelLabel = input('Cancelar');
  readonly severity = input<ConfirmSeverity>('primary');
  readonly icon = input('pi-question-circle');
  readonly busy = input(false);
  readonly confirmDisabled = input(false);
  readonly confirmed = output<void>();
}
