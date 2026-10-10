import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { LoaderScreenComponent } from '@/shared/components/loader-screen/loader-screen.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule, LoaderScreenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
})
export class App {}
