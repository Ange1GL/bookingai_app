import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { buildPageItems, PAGE_GAP } from '../../utils/pagination.util';

/** Pill-style paginator. `page` is zero-based; compact "Página x de y" on phones, numbered on wider screens. */
@Component({
  selector: 'app-pager',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (total() > 0) {
      <nav class="flex flex-col items-center gap-3 sm:flex-row sm:justify-between" aria-label="Paginación">
        <p class="m-0 text-sm text-muted-color" aria-live="polite">
          Mostrando <span class="font-medium text-color">{{ from() }}–{{ to() }}</span> de {{ total() }}
        </p>

        <div class="flex items-center gap-1.5">
          <button
            type="button"
            class="flex size-10 cursor-pointer items-center justify-center rounded-full border border-surface text-muted-color transition hover:bg-surface-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-surface-800"
            aria-label="Página anterior"
            [disabled]="disabled() || page() === 0"
            (click)="go(page() - 1)"
          >
            <i class="pi pi-chevron-left text-sm" aria-hidden="true"></i>
          </button>

          <span class="px-3 text-sm font-medium tabular-nums sm:hidden">{{ page() + 1 }} / {{ totalPages() }}</span>

          <ul class="m-0 hidden list-none items-center gap-1.5 p-0 sm:flex">
            @for (item of items(); track $index) {
              <li>
                @if (item === gap) {
                  <span class="flex size-10 items-center justify-center text-muted-color" aria-hidden="true">…</span>
                } @else {
                  <button
                    type="button"
                    class="flex size-10 cursor-pointer items-center justify-center rounded-full text-sm font-medium tabular-nums transition active:scale-95 disabled:cursor-not-allowed"
                    [class]="
                      item === page()
                        ? 'bg-linear-to-br from-primary-500 to-primary-700 text-white shadow-md shadow-primary-500/30'
                        : 'text-muted-color hover:bg-surface-100 dark:hover:bg-surface-800'
                    "
                    [attr.aria-label]="'Página ' + (item + 1)"
                    [attr.aria-current]="item === page() ? 'page' : null"
                    [disabled]="disabled()"
                    (click)="go(item)"
                  >
                    {{ item + 1 }}
                  </button>
                }
              </li>
            }
          </ul>

          <button
            type="button"
            class="flex size-10 cursor-pointer items-center justify-center rounded-full border border-surface text-muted-color transition hover:bg-surface-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-surface-800"
            aria-label="Página siguiente"
            [disabled]="disabled() || page() >= totalPages() - 1"
            (click)="go(page() + 1)"
          >
            <i class="pi pi-chevron-right text-sm" aria-hidden="true"></i>
          </button>
        </div>
      </nav>
    }
  `,
})
export class PagerComponent {
  readonly page = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly total = input.required<number>();
  readonly disabled = input(false);
  readonly pageChange = output<number>();

  protected readonly gap = PAGE_GAP;
  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.pageSize())));
  protected readonly items = computed(() => buildPageItems(this.page(), this.totalPages()));
  protected readonly from = computed(() => this.page() * this.pageSize() + 1);
  protected readonly to = computed(() => Math.min((this.page() + 1) * this.pageSize(), this.total()));

  protected go(target: number): void {
    if (target !== this.page() && target >= 0 && target < this.totalPages()) {
      this.pageChange.emit(target);
    }
  }
}
