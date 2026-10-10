import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PriceCatalogItemDto } from '../../models/price-catalog.dto';

@Component({
  selector: 'app-price-service-card',
  imports: [ButtonModule, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './price-service-card.component.html',
})
export class PriceServiceCardComponent {
  readonly service = input.required<PriceCatalogItemDto>();
  readonly busy = input(false);
  readonly editRequested = output<PriceCatalogItemDto>();
  readonly deleteRequested = output<PriceCatalogItemDto>();
}
