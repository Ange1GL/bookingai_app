import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CustomerListItemDto } from '../../models/customer.dto';

const INITIALS_COUNT = 2;

@Component({
  selector: 'app-customer-card',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article
      class="flex items-center gap-3 rounded-2xl border border-surface bg-surface-0 p-3 shadow-sm md:p-4 dark:bg-surface-900"
      [class.opacity-60]="busy()"
    >
      <span
        class="flex size-11 shrink-0 items-center justify-center rounded-2xl text-sm font-semibold text-white"
        [class]="customer().blacklisted ? 'bg-rose-500' : 'bg-linear-to-br from-primary-400 to-primary-700'"
        aria-hidden="true"
      >
        {{ initials() }}
      </span>

      <div class="min-w-0 flex-1">
        <p class="m-0 truncate font-semibold">{{ customer().name }}</p>
        <p class="m-0 flex items-center gap-1.5 truncate text-sm text-muted-color">
          <i class="pi pi-phone text-xs" aria-hidden="true"></i>{{ customer().phone }}
        </p>
        @if (customer().blacklisted) {
          <span
            class="mt-1 inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-700 dark:bg-rose-500/10 dark:text-rose-300"
          >
            <i class="pi pi-ban text-[10px]" aria-hidden="true"></i>Lista negra
          </span>
        }
      </div>

      @if (customer().blacklisted) {
        <p-button
          icon="pi pi-undo"
          label="Desbloquear"
          size="small"
          severity="secondary"
          [rounded]="true"
          [loading]="busy()"
          [attr.aria-label]="'Quitar a ' + customer().name + ' de la lista negra'"
          (onClick)="unblockRequested.emit(customer())"
        />
      } @else {
        <p-button
          icon="pi pi-ban"
          label="Bloquear"
          size="small"
          severity="danger"
          [text]="true"
          [rounded]="true"
          [disabled]="busy()"
          [attr.aria-label]="'Agregar a ' + customer().name + ' a la lista negra'"
          (onClick)="blacklistRequested.emit(customer())"
        />
      }
    </article>
  `,
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
