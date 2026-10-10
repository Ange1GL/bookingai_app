import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LoaderService } from '@/core/service/loader.service';

@Component({
  selector: 'app-loader-screen',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loader-screen.component.html',
})
export class LoaderScreenComponent {
  protected readonly loader = inject(LoaderService);
}
