import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { SearchInputComponent } from '@/shared/components/search-input/search-input.component';
import { USER_SEARCH_MAX_LENGTH } from '../../models/user.dto';
import { ActiveFilter } from '../../models/user-query.model';

interface FilterOption {
  value: ActiveFilter;
  label: string;
}

const FILTER_OPTIONS: FilterOption[] = [
  { value: 'all', label: 'Todos' },
  { value: 'active', label: 'Activos' },
  { value: 'inactive', label: 'Inactivos' },
];

@Component({
  selector: 'app-user-filters',
  imports: [SearchInputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-filters.component.html',
})
export class UserFiltersComponent {
  readonly status = input.required<ActiveFilter>();
  readonly searchChange = output<string>();
  readonly statusChange = output<ActiveFilter>();

  protected readonly options = FILTER_OPTIONS;
  protected readonly searchMaxLength = USER_SEARCH_MAX_LENGTH;
}
