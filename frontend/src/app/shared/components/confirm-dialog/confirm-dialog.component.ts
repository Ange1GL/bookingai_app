import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

export type ConfirmSeverity = 'primary' | 'danger';

/** Modal asking for confirmation. Extra fields (e.g. a reason) can be projected as content. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [DialogModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-dialog
      [(visible)]="visible"
      [header]="header()"
      [modal]="true"
      [draggable]="false"
      [closable]="!busy()"
      [closeOnEscape]="!busy()"
      [dismissableMask]="!busy()"
      [style]="{ width: '26rem', maxWidth: '92vw' }"
      role="alertdialog"
    >
      <div class="flex flex-col gap-4">
        <div class="flex items-start gap-3">
          <span
            class="flex size-10 shrink-0 items-center justify-center rounded-full"
            [class]="severity() === 'danger' ? 'bg-rose-50 text-rose-600 dark:bg-rose-500/10' : 'bg-primary-50 text-primary dark:bg-primary-500/10'"
            aria-hidden="true"
          >
            <i class="pi" [class]="icon()"></i>
          </span>
          <p class="m-0 text-sm leading-relaxed">{{ message() }}</p>
        </div>
        <ng-content />
      </div>

      <ng-template #footer>
        <p-button [label]="cancelLabel()" severity="secondary" [text]="true" [disabled]="busy()" (onClick)="visible.set(false)" />
        <p-button
          [label]="confirmLabel()"
          [severity]="severity() === 'danger' ? 'danger' : undefined"
          [loading]="busy()"
          [disabled]="confirmDisabled()"
          (onClick)="confirmed.emit()"
        />
      </ng-template>
    </p-dialog>
  `,
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
