import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LoaderService } from '@/core/service/loader.service';

@Component({
  selector: 'app-loader-screen',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loader.isLoading()) {
      <div class="fixed inset-0 z-9999 flex items-center justify-center bg-black/30" role="status" aria-label="Cargando">
        <div class="flex h-20 w-20 animate-spin items-center justify-center rounded-full border-4 border-transparent border-t-violet-500">
          <div class="h-16 w-16 animate-spin rounded-full border-4 border-transparent border-t-violet-300"></div>
        </div>
      </div>
    }
  `,
})
export class LoaderScreenComponent {
  protected readonly loader = inject(LoaderService);
}
