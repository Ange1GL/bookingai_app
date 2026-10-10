import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PriceCatalogItemDto } from '../../models/price-catalog.dto';

@Component({
  selector: 'app-price-service-card',
  imports: [ButtonModule, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article
      class="flex items-center gap-3 rounded-2xl border border-surface bg-surface-0 p-3 shadow-sm md:p-4 dark:bg-surface-900"
      [class.opacity-60]="busy()"
    >
      <span
        class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-linear-to-br from-primary-400 to-primary-700 text-white"
        aria-hidden="true"
      >
        <i class="pi pi-tag"></i>
      </span>

      <div class="min-w-0 flex-1">
        <p class="m-0 truncate font-semibold">{{ service().label }}</p>
        <p class="m-0 text-sm font-medium tabular-nums text-primary">
          {{ service().price | currency: 'MXN' : 'symbol-narrow' : '1.0-0' }}
        </p>
      </div>

      <div class="flex shrink-0 items-center gap-1">
        <p-button
          icon="pi pi-pencil"
          severity="secondary"
          [text]="true"
          [rounded]="true"
          [disabled]="busy()"
          [attr.aria-label]="'Editar ' + service().label"
          (onClick)="editRequested.emit(service())"
        />
        <p-button
          icon="pi pi-trash"
          severity="danger"
          [text]="true"
          [rounded]="true"
          [loading]="busy()"
          [attr.aria-label]="'Eliminar ' + service().label"
          (onClick)="deleteRequested.emit(service())"
        />
      </div>
    </article>
  `,
})
export class PriceServiceCardComponent {
  readonly service = input.required<PriceCatalogItemDto>();
  readonly busy = input(false);
  readonly editRequested = output<PriceCatalogItemDto>();
  readonly deleteRequested = output<PriceCatalogItemDto>();
}
