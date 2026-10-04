import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { SessionService } from '@/core/service/session.service';

@Component({
  selector: 'app-home-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Hola, {{ session.user()?.username }}</h1>
    <p class="text-muted-color">Aquí irán las features de BookingApp.</p>
  `,
})
export class HomePageComponent {
  protected readonly session = inject(SessionService);
}
