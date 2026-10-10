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
  templateUrl: './price-catalog-page.component.html',
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
