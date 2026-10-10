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
  template: `
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div class="min-w-0 flex-1">
        <app-search-input
          placeholder="Buscar por nombre o teléfono"
          label="Buscar clientes por nombre o teléfono"
          [maxLength]="nameMaxLength"
          (searched)="searchChange.emit($event)"
        />
      </div>
      <div class="flex gap-2" role="group" aria-label="Filtrar clientes">
        @for (option of options; track option.value) {
          <button
            type="button"
            class="cursor-pointer rounded-full border px-4 py-2 text-sm font-medium transition"
            [class]="
              blacklist() === option.value
                ? 'border-primary bg-primary-50 text-primary dark:bg-primary-500/10'
                : 'border-surface text-muted-color hover:bg-surface-100 dark:hover:bg-surface-800'
            "
            [attr.aria-pressed]="blacklist() === option.value"
            (click)="blacklistChange.emit(option.value)"
          >
            {{ option.label }}
          </button>
        }
      </div>
    </div>
  `,
})
export class CustomerFiltersComponent {
  readonly blacklist = input.required<BlacklistFilter>();
  readonly searchChange = output<string>();
  readonly blacklistChange = output<BlacklistFilter>();

  protected readonly options = FILTER_OPTIONS;
  protected readonly nameMaxLength = NAME_MAX_LENGTH;
}
