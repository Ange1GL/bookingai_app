import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { LABEL_MAX_LENGTH, PRICE_MAX, PRICE_MIN, PriceCatalogItemDto, PriceCatalogRequestDto } from '../../models/price-catalog.dto';
import { PriceServiceForm } from '../../models/price-service-form.model';

/** Creates a service, or edits `service` when one is provided. */
@Component({
  selector: 'app-price-service-form-dialog',
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule, InputNumberModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-dialog
      [(visible)]="visible"
      [header]="header()"
      [modal]="true"
      [draggable]="false"
      [closable]="!saving()"
      [closeOnEscape]="!saving()"
      [style]="{ width: '26rem', maxWidth: '92vw' }"
    >
      <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4" novalidate>
        <div class="flex flex-col gap-2">
          <label for="service-label" class="text-sm font-medium">Nombre del servicio</label>
          <input
            pInputText
            id="service-label"
            formControlName="label"
            autocomplete="off"
            placeholder="Corte + barba"
            [attr.maxlength]="labelMaxLength"
            [attr.aria-invalid]="form.controls.label.touched && form.controls.label.invalid"
          />
          @if (form.controls.label.touched && form.controls.label.hasError('required')) {
            <small class="text-red-500">El nombre es obligatorio.</small>
          }
        </div>

        <div class="flex flex-col gap-2">
          <label for="service-price" class="text-sm font-medium">Precio</label>
          <p-inputnumber
            inputId="service-price"
            formControlName="price"
            mode="currency"
            currency="MXN"
            locale="es-MX"
            [minFractionDigits]="0"
            [maxFractionDigits]="0"
            [min]="priceMin"
            [max]="priceMax"
            placeholder="$90"
            styleClass="w-full"
            inputStyleClass="w-full"
          />
          @if (form.controls.price.touched) {
            @if (form.controls.price.hasError('required')) {
              <small class="text-red-500">El precio es obligatorio.</small>
            } @else if (form.controls.price.hasError('min')) {
              <small class="text-red-500">El precio debe ser mayor a 0.</small>
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
export class PriceServiceFormDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly visible = model(false);
  /** Service being edited; `null` means create mode. */
  readonly service = input<PriceCatalogItemDto | null>(null);
  readonly saving = input(false);
  readonly submitted = output<PriceCatalogRequestDto>();

  protected readonly labelMaxLength = LABEL_MAX_LENGTH;
  protected readonly priceMin = PRICE_MIN;
  protected readonly priceMax = PRICE_MAX;
  protected readonly header = computed(() => (this.service() ? 'Editar servicio' : 'Nuevo servicio'));
  protected readonly form: FormGroup<PriceServiceForm> = this.fb.group({
    label: this.fb.control('', [Validators.required, Validators.maxLength(LABEL_MAX_LENGTH)]),
    price: this.fb.control<number | null>(null, [Validators.required, Validators.min(PRICE_MIN), Validators.max(PRICE_MAX)]),
  });

  constructor() {
    effect(() => {
      if (!this.visible()) {
        return;
      }
      const service = this.service();
      this.form.reset({ label: service?.label ?? '', price: service?.price ?? null });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { label, price } = this.form.getRawValue();
    this.submitted.emit({ label: label.trim(), price: price as number });
  }
}
