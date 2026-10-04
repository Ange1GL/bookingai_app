import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { LoaderService } from '@/core/service/loader.service';

@Component({
  selector: 'app-loader-screen',
  imports: [ProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loader.isLoading()) {
      <div class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/30" role="status" aria-label="Cargando">
        <p-progress-spinner strokeWidth="4" />
      </div>
    }
  `,
})
export class LoaderScreenComponent {
  protected readonly loader = inject(LoaderService);
}
