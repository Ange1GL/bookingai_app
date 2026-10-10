import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { SearchInputComponent } from '@/shared/components/search-input/search-input.component';
import { NAME_MAX_LENGTH } from '../../models/customer.dto';
import { BlacklistFilter } from '../../models/customer-query.model';

interface FilterOption {
  value: BlacklistFilter;
  label: string;
}

const FILTER_OPTIONS: FilterOption[] = [
  { value: 'all', label: 'Todos' },
  { value: 'blacklisted', label: 'Lista negra' },
];

@Component({
  selector: 'app-customer-filters',
  imports: [SearchInputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-filters.component.html',
})
export class CustomerFiltersComponent {
  readonly blacklist = input.required<BlacklistFilter>();
  readonly searchChange = output<string>();
  readonly blacklistChange = output<BlacklistFilter>();

  protected readonly options = FILTER_OPTIONS;
  protected readonly nameMaxLength = NAME_MAX_LENGTH;
}
