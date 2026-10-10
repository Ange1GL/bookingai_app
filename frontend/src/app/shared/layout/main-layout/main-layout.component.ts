import { IMAGE_LOADER, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SessionService } from '@/core/service/session.service';
import { widthVariantImageLoader } from '@/shared/utils/width-variant-image-loader';

interface NavItem {
  label: string;
  icon: string;
  path: string;
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
  protected readonly navItems = NAV_ITEMS;
  protected readonly logoSrc = LOGO_SRC;
  protected readonly logoWidth = LOGO_WIDTH;
  protected readonly logoHeight = LOGO_HEIGHT;

  protected logout(): void {
    this.session.logout().subscribe({ error: () => undefined });
  }
}
