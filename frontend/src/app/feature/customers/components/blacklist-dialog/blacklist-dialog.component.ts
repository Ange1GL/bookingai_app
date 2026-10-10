import { ChangeDetectionStrategy, Component, computed, effect, input, model, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { BLACKLIST_REASON_MAX_LENGTH, CustomerListItemDto } from '../../models/customer.dto';

/** Asks for confirmation (and an optional reason) before blocking a customer. */
@Component({
  selector: 'app-blacklist-dialog',
  imports: [ReactiveFormsModule, TextareaModule, ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-confirm-dialog
      [(visible)]="visible"
      header="Agregar a lista negra"
      icon="pi-ban"
      severity="danger"
      confirmLabel="Sí, bloquear"
      [message]="message()"
      [busy]="busy()"
      [confirmDisabled]="reason.invalid"
      (confirmed)="confirm()"
    >
      <div class="flex flex-col gap-2">
        <label for="blacklist-reason" class="text-sm font-medium">Motivo <span class="text-muted-color">(opcional)</span></label>
        <textarea
          pTextarea
          id="blacklist-reason"
          rows="3"
          class="w-full"
          [formControl]="reason"
          [attr.maxlength]="maxLength"
        ></textarea>
        <small class="self-end tabular-nums text-muted-color">{{ length() }}/{{ maxLength }}</small>
      </div>
    </app-confirm-dialog>
  `,
})
export class BlacklistDialogComponent {
  readonly customer = input<CustomerListItemDto | null>(null);
  readonly visible = model(false);
  readonly busy = input(false);
  /** Trimmed reason, or `null` when left empty. */
  readonly confirmed = output<string | null>();

  protected readonly maxLength = BLACKLIST_REASON_MAX_LENGTH;
  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(BLACKLIST_REASON_MAX_LENGTH)],
  });
  protected readonly length = toSignal(this.reason.valueChanges.pipe(map((value) => value.length)), { initialValue: 0 });

  protected readonly message = computed(
    () =>
      `${this.customer()?.name ?? 'El cliente'} no podrá reservar y sus citas futuras se cancelarán. Podrás quitarlo de la lista negra después.`,
  );

  constructor() {
    effect(() => {
      if (!this.visible()) {
        this.reason.reset();
      }
    });
  }

  protected confirm(): void {
    this.confirmed.emit(this.reason.value.trim() || null);
  }
}
