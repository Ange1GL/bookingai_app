import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';
import { PriceServiceCardComponent } from '../../components/price-service-card/price-service-card.component';
import { PriceServiceFormDialogComponent } from '../../components/price-service-form-dialog/price-service-form-dialog.component';
import { PriceCatalogItemDto, PriceCatalogRequestDto } from '../../models/price-catalog.dto';
import { PriceCatalogService } from '../../service/price-catalog.service';

const SKELETON_ROWS = [0, 1, 2, 3];
const TOAST_LIFE_MS = 3000;

@Component({
  selector: 'app-price-catalog-page',
  imports: [ButtonModule, SkeletonModule, ConfirmDialogComponent, PriceServiceCardComponent, PriceServiceFormDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto flex w-full max-w-3xl flex-col gap-4 md:gap-6">
      <header class="flex items-center justify-between gap-3">
        <div>
          <h1 class="m-0 text-2xl font-bold tracking-tight">Catálogo de precios</h1>
          <p class="m-0 text-sm text-muted-color">{{ totalLabel() }}</p>
        </div>
        <p-button label="Nuevo" icon="pi pi-plus" [rounded]="true" (onClick)="openCreate()" />
      </header>

      @if (failed()) {
        <div
          class="flex items-center justify-between gap-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300"
          role="alert"
        >
          <span class="flex items-center gap-2"><i class="pi pi-exclamation-triangle" aria-hidden="true"></i>No se pudo cargar el catálogo.</span>
          <p-button label="Reintentar" size="small" severity="secondary" [text]="true" (onClick)="reload()" />
        </div>
      }

      <section aria-label="Servicios del catálogo" [attr.aria-busy]="loading()">
        @if (services().length === 0) {
          @if (loading()) {
            <div class="flex flex-col gap-2">
              @for (row of skeletonRows; track row) {
                <p-skeleton height="4.5rem" borderRadius="1rem" />
              }
            </div>
          } @else if (!failed()) {
            <div class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-surface px-4 py-10 text-center text-muted-color">
              <span class="flex size-12 items-center justify-center rounded-full bg-primary-50 text-primary dark:bg-primary-500/10">
                <i class="pi pi-tag text-xl" aria-hidden="true"></i>
              </span>
              <p class="m-0 font-medium">Aún no tienes servicios</p>
              <p class="m-0 text-sm">Crea tu primer servicio con el botón Nuevo para poder agendar citas.</p>
            </div>
          }
        } @else {
          <ul class="m-0 flex list-none flex-col gap-2 p-0 transition-opacity" [class.opacity-60]="loading()">
            @for (service of services(); track service.id) {
              <li>
                <app-price-service-card
                  [service]="service"
                  [busy]="deletingId() === service.id"
                  (editRequested)="openEdit($event)"
                  (deleteRequested)="askDelete($event)"
                />
              </li>
            }
          </ul>
        }
      </section>
    </div>

    <app-price-service-form-dialog
      [(visible)]="formVisible"
      [service]="target()"
      [saving]="saving()"
      (submitted)="save($event)"
    />

    <app-confirm-dialog
      [(visible)]="deleteVisible"
      header="Eliminar servicio"
      icon="pi-trash"
      severity="danger"
      confirmLabel="Sí, eliminar"
      [message]="deleteMessage()"
      [busy]="deletingId() !== null"
      (confirmed)="remove()"
    />
  `,
})
export class PriceCatalogPageComponent {
  private readonly catalog = inject(PriceCatalogService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly formVisible = signal(false);
  protected readonly deleteVisible = signal(false);
  /** Service being edited or deleted; `null` while creating. */
  protected readonly target = signal<PriceCatalogItemDto | null>(null);
  protected readonly saving = signal(false);
  protected readonly deletingId = signal<number | null>(null);

  private readonly resource = rxResource({ stream: () => this.catalog.list() });

  /** Keeps the last good list on screen while a reload is in flight, avoiding skeleton flicker. */
  private readonly items = linkedSignal<PriceCatalogItemDto[] | undefined, PriceCatalogItemDto[] | undefined>({
    source: () => (this.resource.hasValue() ? this.resource.value() : undefined),
    computation: (value, previous) => value ?? previous?.value,
  });

  protected readonly loading = this.resource.isLoading;
  protected readonly failed = computed(() => this.resource.error() !== undefined);
  protected readonly services = computed(() => this.items() ?? []);
  protected readonly totalLabel = computed(() => {
    const total = this.services().length;
    return total === 1 ? '1 servicio' : `${total} servicios`;
  });
  protected readonly deleteMessage = computed(
    () =>
      `¿Eliminar "${this.target()?.label ?? 'este servicio'}"? Ya no podrás asignarlo a citas nuevas; las citas existentes lo seguirán mostrando.`,
  );

  protected reload(): void {
    this.resource.reload();
  }

  protected openCreate(): void {
    this.target.set(null);
    this.formVisible.set(true);
  }

  protected openEdit(service: PriceCatalogItemDto): void {
    this.target.set(service);
    this.formVisible.set(true);
  }

  protected askDelete(service: PriceCatalogItemDto): void {
    this.target.set(service);
    this.deleteVisible.set(true);
  }

  protected save(request: PriceCatalogRequestDto): void {
    const editing = this.target();
    const call = editing ? this.catalog.update(editing.id, request) : this.catalog.create(request);
    this.run(call, (busy) => this.saving.set(busy), this.formVisible, () => (editing ? `${request.label} actualizado` : `${request.label} creado`));
  }

  protected remove(): void {
    const service = this.target();
    if (!service || this.deletingId() !== null) {
      return;
    }
    this.run(
      this.catalog.remove(service.id),
      (busy) => this.deletingId.set(busy ? service.id : null),
      this.deleteVisible,
      () => `${service.label} eliminado`,
    );
  }

  private run(
    request: Observable<unknown>,
    setBusy: (busy: boolean) => void,
    dialog: { set(open: boolean): void },
    successMessage: () => string,
  ): void {
    setBusy(true);
    request
      .pipe(
        finalize(() => setBusy(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          dialog.set(false);
          this.messages.add({ severity: 'success', summary: 'Listo', detail: successMessage(), life: TOAST_LIFE_MS });
          this.reload();
        },
        // errorInterceptor already shows the toast; keep the dialog open so the user can retry.
        error: () => undefined,
      });
  }
}
