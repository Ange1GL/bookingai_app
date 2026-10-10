import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CustomerListItemDto } from '../../models/customer.dto';

const INITIALS_COUNT = 2;

@Component({
  selector: 'app-customer-card',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-card.component.html',
})
export class CustomerCardComponent {
  readonly customer = input.required<CustomerListItemDto>();
  readonly busy = input(false);
  readonly blacklistRequested = output<CustomerListItemDto>();
  readonly unblockRequested = output<CustomerListItemDto>();

  protected readonly initials = computed(() =>
    this.customer()
      .name.split(/\s+/)
      .filter(Boolean)
      .slice(0, INITIALS_COUNT)
      .map((word) => word[0]?.toUpperCase())
      .join(''),
  );
}
