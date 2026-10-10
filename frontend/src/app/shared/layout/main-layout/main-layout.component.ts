import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SessionService } from '@/core/service/session.service';

interface NavItem {
  label: string;
  icon: string;
  path: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Inicio', icon: 'pi-home', path: '/home' },
  { label: 'Calendario', icon: 'pi-calendar', path: '/appointments' },
  { label: 'Clientes', icon: 'pi-users', path: '/customers' },
  { label: 'Precios', icon: 'pi-tag', path: '/price-catalog' },
  { label: 'Asistente', icon: 'pi-sparkles', path: '/assistant' },
];

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header
      class="sticky top-0 z-20 flex items-center justify-between gap-3 border-b border-surface bg-surface-0/80 px-4 py-3 backdrop-blur md:px-6 dark:bg-surface-950/80"
    >
      <a routerLink="/home" class="flex items-center gap-2 no-underline" aria-label="BookingAI, inicio">
        <span
          class="flex size-9 items-center justify-center rounded-xl bg-linear-to-br from-primary-400 to-primary-700 text-white shadow shadow-primary-500/30"
          aria-hidden="true"
        >
          <i class="pi pi-calendar-clock"></i>
        </span>
        <span class="text-lg font-bold tracking-tight text-color">booking<span class="text-primary">AI</span></span>
      </a>

      <nav class="hidden items-center gap-1 md:flex" aria-label="Principal">
        @for (item of navItems; track item.path) {
          <a
            [routerLink]="item.path"
            routerLinkActive="bg-primary-50 text-primary dark:bg-primary-500/10"
            class="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-muted-color no-underline transition hover:bg-surface-100 dark:hover:bg-surface-800"
          >
            <i class="pi" [class]="item.icon" aria-hidden="true"></i>{{ item.label }}
          </a>
        }
      </nav>

      <div class="flex items-center gap-3">
        <span class="hidden text-muted-color sm:inline">{{ session.user()?.username }}</span>
        <p-button icon="pi pi-sign-out" label="Salir" severity="secondary" size="small" [rounded]="true" (onClick)="logout()" />
      </div>
    </header>

    <main class="p-4 pb-28 md:p-6 md:pb-6">
      <router-outlet />
    </main>

    <nav
      class="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-surface bg-surface-0/90 px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] backdrop-blur md:hidden dark:bg-surface-950/90"
      aria-label="Principal"
    >
      @for (item of navItems; track item.path) {
        <a
          [routerLink]="item.path"
          routerLinkActive="text-primary"
          #active="routerLinkActive"
          class="flex min-h-14 flex-col items-center justify-center gap-0.5 rounded-2xl text-xs font-medium text-muted-color no-underline transition"
          [attr.aria-current]="active.isActive ? 'page' : null"
        >
          <span
            class="flex h-8 w-14 items-center justify-center rounded-full transition"
            [class]="active.isActive ? 'bg-primary-50 dark:bg-primary-500/15' : ''"
          >
            <i class="pi text-lg" [class]="item.icon" aria-hidden="true"></i>
          </span>
          {{ item.label }}
        </a>
      }
    </nav>
  `,
})
export class MainLayoutComponent {
  protected readonly session = inject(SessionService);
  protected readonly navItems = NAV_ITEMS;

  protected logout(): void {
    this.session.logout().subscribe({ error: () => undefined });
  }
}
