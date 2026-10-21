import { IMAGE_LOADER, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { Permission, PERMISSIONS } from '@/core/security/permissions';
import { SessionService } from '@/core/service/session.service';
import { widthVariantImageLoader } from '@/shared/utils/width-variant-image-loader';

interface NavItem {
  label: string;
  icon: string;
  path: string;
  /** Shown only to users holding any of these permissions (ADMIN_ALL included); omit for everyone. */
  permissions?: readonly Permission[];
}

const LOGO_SRC = 'logos/logo-horizontal.webp';
const LOGO_WIDTH = 115;
const LOGO_HEIGHT = 40;

const NAV_ITEMS: NavItem[] = [
  { label: 'Inicio', icon: 'pi-home', path: '/home' },
  { label: 'Calendario', icon: 'pi-calendar', path: '/appointments' },
  { label: 'Clientes', icon: 'pi-users', path: '/customers' },
  { label: 'Precios', icon: 'pi-tag', path: '/price-catalog' },
  { label: 'Asistente', icon: 'pi-sparkles', path: '/assistant' },
  { label: 'Usuarios', icon: 'pi-id-card', path: '/users', permissions: [PERMISSIONS.USER_READ] },
];

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, NgOptimizedImage],
  providers: [{ provide: IMAGE_LOADER, useValue: widthVariantImageLoader }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  protected readonly session = inject(SessionService);
  protected readonly navItems = computed(() =>
    NAV_ITEMS.filter((item) => this.session.hasAnyPermission(item.permissions ?? [])),
  );
  protected readonly logoSrc = LOGO_SRC;
  protected readonly logoWidth = LOGO_WIDTH;
  protected readonly logoHeight = LOGO_HEIGHT;

  protected logout(): void {
    this.session.logout().subscribe({ error: () => undefined });
  }
}
