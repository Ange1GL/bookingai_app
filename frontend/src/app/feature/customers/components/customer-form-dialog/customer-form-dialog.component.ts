import { ChangeDetectionStrategy, Component, inject, input, model, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import {
  CreateCustomerRequestDto,
  NAME_MAX_LENGTH,
  PHONE_MAX_LENGTH,
  PHONE_PATTERN,
} from '../../models/customer.dto';
import { CustomerForm } from '../../models/customer-form.model';

@Component({
  selector: 'app-customer-form-dialog',
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-dialog
      [(visible)]="visible"
      header="Nuevo cliente"
      [modal]="true"
      [draggable]="false"
      [closable]="!saving()"
      [closeOnEscape]="!saving()"
      [style]="{ width: '26rem', maxWidth: '92vw' }"
      (onHide)="form.reset()"
    >
      <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4" novalidate>
        <div class="flex flex-col gap-2">
          <label for="customer-name" class="text-sm font-medium">Nombre</label>
          <input
            pInputText
            id="customer-name"
            formControlName="name"
            autocomplete="off"
            placeholder="Ana López"
            [attr.maxlength]="nameMaxLength"
            [attr.aria-invalid]="form.controls.name.touched && form.controls.name.invalid"
          />
          @if (form.controls.name.touched && form.controls.name.hasError('required')) {
            <small class="text-red-500">El nombre es obligatorio.</small>
          }
        </div>

        <div class="flex flex-col gap-2">
          <label for="customer-phone" class="text-sm font-medium">Teléfono</label>
          <input
            pInputText
            id="customer-phone"
            type="tel"
            formControlName="phone"
            autocomplete="off"
            placeholder="55 1234 5678"
            [attr.maxlength]="phoneMaxLength"
            [attr.aria-invalid]="form.controls.phone.touched && form.controls.phone.invalid"
          />
          @if (form.controls.phone.touched) {
            @if (form.controls.phone.hasError('required')) {
              <small class="text-red-500">El teléfono es obligatorio.</small>
            } @else if (form.controls.phone.hasError('pattern')) {
              <small class="text-red-500">Usa solo dígitos, espacios, guiones, paréntesis o un + inicial.</small>
            }
          }
        </div>

        <div class="flex justify-end gap-2 pt-2">
          <p-button label="Cancelar" severity="secondary" [text]="true" [disabled]="saving()" (onClick)="visible.set(false)" />
          <p-button type="submit" label="Guardar" icon="pi pi-check" [loading]="saving()" />
        </div>
      </form>
    </p-dialog>
  `,
})
export class CustomerFormDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly visible = model(false);
  readonly saving = input(false);
  readonly submitted = output<CreateCustomerRequestDto>();

  protected readonly nameMaxLength = NAME_MAX_LENGTH;
  protected readonly phoneMaxLength = PHONE_MAX_LENGTH;
  protected readonly form: FormGroup<CustomerForm> = this.fb.group({
    name: this.fb.control('', [Validators.required, Validators.maxLength(NAME_MAX_LENGTH)]),
    phone: this.fb.control('', [Validators.required, Validators.maxLength(PHONE_MAX_LENGTH), Validators.pattern(PHONE_PATTERN)]),
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, phone } = this.form.getRawValue();
    this.submitted.emit({ name: name.trim(), phone: phone.trim() });
  }
}
