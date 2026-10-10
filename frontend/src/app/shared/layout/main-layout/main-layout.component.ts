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
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  protected readonly session = inject(SessionService);
  protected readonly navItems = NAV_ITEMS;

  protected logout(): void {
    this.session.logout().subscribe({ error: () => undefined });
  }
}
