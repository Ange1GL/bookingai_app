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
  templateUrl: './price-service-form-dialog.component.html',
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
