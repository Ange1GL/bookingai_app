import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { PagerComponent } from '@/shared/components/pager/pager.component';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { BlacklistDialogComponent } from '../../components/blacklist-dialog/blacklist-dialog.component';
import { CustomerCardComponent } from '../../components/customer-card/customer-card.component';
import { CustomerFiltersComponent } from '../../components/customer-filters/customer-filters.component';
import { CustomerFormDialogComponent } from '../../components/customer-form-dialog/customer-form-dialog.component';
import { CreateCustomerRequestDto, CustomerListItemDto, CustomerPageDto, PAGE_SIZE } from '../../models/customer.dto';
import { BlacklistFilter, CustomerQuery, INITIAL_FILTERS } from '../../models/customer-query.model';
import { CustomersService } from '../../service/customers.service';

const SKELETON_ROWS = [0, 1, 2, 3];
const TOAST_LIFE_MS = 3000;

@Component({
  selector: 'app-customers-page',
  imports: [
    ButtonModule,
    SkeletonModule,
    ConfirmDialogComponent,
    PagerComponent,
    BlacklistDialogComponent,
    CustomerCardComponent,
    CustomerFiltersComponent,
    CustomerFormDialogComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto flex w-full max-w-3xl flex-col gap-4 md:gap-6">
      <header class="flex items-center justify-between gap-3">
        <div>
          <h1 class="m-0 text-2xl font-bold tracking-tight">Clientes</h1>
          <p class="m-0 text-sm text-muted-color">{{ totalLabel() }}</p>
        </div>
        <p-button label="Nuevo" icon="pi pi-plus" [rounded]="true" (onClick)="formVisible.set(true)" />
      </header>

      <app-customer-filters
        [blacklist]="query().blacklist"
        (searchChange)="setSearch($event)"
        (blacklistChange)="setBlacklist($event)"
      />

      @if (failed()) {
        <div
          class="flex items-center justify-between gap-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300"
          role="alert"
        >
          <span class="flex items-center gap-2"><i class="pi pi-exclamation-triangle" aria-hidden="true"></i>No se pudieron cargar los clientes.</span>
          <p-button label="Reintentar" size="small" severity="secondary" [text]="true" (onClick)="reload()" />
        </div>
      }

      <section aria-label="Listado de clientes" [attr.aria-busy]="loading()">
        @if (customers().length === 0) {
          @if (loading()) {
            <div class="flex flex-col gap-2">
              @for (row of skeletonRows; track row) {
                <p-skeleton height="4.5rem" borderRadius="1rem" />
              }
            </div>
          } @else if (!failed()) {
            <div class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-surface px-4 py-10 text-center text-muted-color">
              <span class="flex size-12 items-center justify-center rounded-full bg-primary-50 text-primary dark:bg-primary-500/10">
                <i class="pi pi-users text-xl" aria-hidden="true"></i>
              </span>
              <p class="m-0 font-medium">{{ hasFilters() ? 'Sin resultados' : 'Aún no tienes clientes' }}</p>
              <p class="m-0 text-sm">{{ hasFilters() ? 'Prueba con otra búsqueda o filtro.' : 'Agrega tu primer cliente con el botón Nuevo.' }}</p>
            </div>
          }
        } @else {
          <ul class="m-0 flex list-none flex-col gap-2 p-0 transition-opacity" [class.opacity-60]="loading()">
            @for (customer of customers(); track customer.id) {
              <li>
                <app-customer-card
                  [customer]="customer"
                  [busy]="busyId() === customer.id"
                  (blacklistRequested)="askBlacklist($event)"
                  (unblockRequested)="askUnblock($event)"
                />
              </li>
            }
          </ul>
        }
      </section>

      <app-pager
        [page]="query().page"
        [pageSize]="pageSize"
        [total]="totalRecords()"
        [disabled]="loading()"
        (pageChange)="changePage($event)"
      />
    </div>

    <app-customer-form-dialog [(visible)]="formVisible" [saving]="creating()" (submitted)="create($event)" />

    <app-blacklist-dialog
      [customer]="target()"
      [(visible)]="blacklistVisible"
      [busy]="busyId() !== null"
      (confirmed)="blacklist($event)"
    />

    <app-confirm-dialog
      [(visible)]="unblockVisible"
      header="Quitar de la lista negra"
      icon="pi-undo"
      confirmLabel="Sí, desbloquear"
      [message]="unblockMessage()"
      [busy]="busyId() !== null"
      (confirmed)="unblock()"
    />
  `,
})
export class CustomersPageComponent {
  private readonly customersService = inject(CustomersService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly query = signal<CustomerQuery>({ ...INITIAL_FILTERS, page: 0 });
  protected readonly formVisible = signal(false);
  protected readonly blacklistVisible = signal(false);
  protected readonly unblockVisible = signal(false);
  protected readonly target = signal<CustomerListItemDto | null>(null);
  protected readonly creating = signal(false);
  protected readonly busyId = signal<number | null>(null);

  private readonly resource = rxResource({
    params: () => this.query(),
    stream: ({ params }) => this.customersService.list(params),
  });

  /** Keeps the last good page on screen while the next one loads, avoiding skeleton flicker. */
  private readonly page = linkedSignal<CustomerPageDto | undefined, CustomerPageDto | undefined>({
    source: () => (this.resource.hasValue() ? this.resource.value() : undefined),
    computation: (value, previous) => value ?? previous?.value,
  });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);
  protected readonly customers = computed(() => this.page()?.content ?? []);
  protected readonly totalRecords = computed(() => this.page()?.totalElements ?? 0);
  protected readonly hasFilters = computed(() => this.query().search !== '' || this.query().blacklist !== 'all');
  protected readonly totalLabel = computed(() => {
    const total = this.totalRecords();
    return total === 1 ? '1 cliente' : `${total} clientes`;
  });
  protected readonly unblockMessage = computed(
    () => `¿Quitar a ${this.target()?.name ?? 'este cliente'} de la lista negra? Podrá volver a reservar citas.`,
  );

  protected setSearch(search: string): void {
    this.query.update((query) => ({ ...query, search, page: 0 }));
  }

  protected setBlacklist(blacklist: BlacklistFilter): void {
    this.query.update((query) => ({ ...query, blacklist, page: 0 }));
  }

  protected changePage(page: number): void {
    this.query.update((query) => ({ ...query, page }));
  }

  protected reload(): void {
    this.resource.reload();
  }

  protected create(request: CreateCustomerRequestDto): void {
    this.creating.set(true);
    this.customersService
      .create(request)
      .pipe(
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (customer) => {
          this.formVisible.set(false);
          this.toast(`${customer.name} guardado`);
          this.reload();
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  protected askBlacklist(customer: CustomerListItemDto): void {
    this.target.set(customer);
    this.blacklistVisible.set(true);
  }

  protected askUnblock(customer: CustomerListItemDto): void {
    this.target.set(customer);
    this.unblockVisible.set(true);
  }

  protected blacklist(reason: string | null): void {
    this.mutate(
      (id) => this.customersService.blacklist(id, reason),
      this.blacklistVisible,
      (name) => `${name} agregado a la lista negra`,
    );
  }

  protected unblock(): void {
    this.mutate(
      (id) => this.customersService.removeFromBlacklist(id),
      this.unblockVisible,
      (name) => `${name} quitado de la lista negra`,
    );
  }

  private mutate(
    request: (id: number) => Observable<void>,
    dialog: { set(open: boolean): void },
    successMessage: (name: string) => string,
  ): void {
    const customer = this.target();
    if (!customer || this.busyId() !== null) {
      return;
    }
    this.busyId.set(customer.id);
    request(customer.id)
      .pipe(
        finalize(() => this.busyId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          dialog.set(false);
          this.toast(successMessage(customer.name));
          this.reload();
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }

  private toast(detail: string): void {
    this.messages.add({ severity: 'success', summary: 'Listo', detail, life: TOAST_LIFE_MS });
  }
}
