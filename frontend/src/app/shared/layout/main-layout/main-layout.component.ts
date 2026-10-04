import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SessionService } from '@/core/service/session.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="flex items-center justify-between border-b border-surface px-6 py-3">
      <span class="text-lg font-semibold">BookingApp</span>
      <div class="flex items-center gap-3">
        <span class="text-muted-color">{{ session.user()?.username }}</span>
        <p-button label="Salir" icon="pi pi-sign-out" severity="secondary" size="small" (onClick)="logout()" />
      </div>
    </header>
    <main class="p-6">
      <router-outlet />
    </main>
  `,
})
export class MainLayoutComponent {
  protected readonly session = inject(SessionService);

  protected logout(): void {
    this.session.logout().subscribe({ error: () => undefined });
  }
}
