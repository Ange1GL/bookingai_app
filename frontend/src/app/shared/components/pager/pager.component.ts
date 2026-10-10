import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { buildPageItems, PAGE_GAP } from '../../utils/pagination.util';

/** Pill-style paginator. `page` is zero-based; compact "Página x de y" on phones, numbered on wider screens. */
@Component({
  selector: 'app-pager',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pager.component.html',
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
