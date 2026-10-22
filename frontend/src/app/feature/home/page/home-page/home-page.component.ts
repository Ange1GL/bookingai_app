import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Permission, PERMISSIONS } from '@/core/security/permissions';
import { SessionService } from '@/core/service/session.service';
import { PushNotificationsCardComponent } from '@/shared/components/push-notifications-card/push-notifications-card.component';

const TODAY_FORMAT = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long' });

interface Shortcut {
  label: string;
  description: string;
  icon: string;
  path: string;
  permissions?: readonly Permission[];
}

interface UpcomingShortcut {
  label: string;
  icon: string;
}

const SHORTCUTS: Shortcut[] = [
  {
    label: 'Mi calendario',
    description: 'Revisa y administra tus citas del mes o la semana.',
    icon: 'pi-calendar',
    path: '/appointments',
  },
  {
    label: 'Clientes',
    description: 'Busca, agrega y administra la lista negra de tus clientes.',
    icon: 'pi-users',
    path: '/customers',
  },
  {
    label: 'Catálogo de precios',
    description: 'Administra los servicios que ofreces y cuánto cobras.',
    icon: 'pi-tag',
    path: '/price-catalog',
  },
  {
    label: 'Asistente de AI',
    description: 'Agenda, mueve o cancela citas conversando.',
    icon: 'pi-sparkles',
    path: '/assistant',
  },
  {
    label: 'Usuarios',
    description: 'Da de alta usuarios, entrega códigos de acceso y atiende recuperaciones.',
    icon: 'pi-id-card',
    path: '/users',
    permissions: [PERMISSIONS.USER_READ],
  },
];

const UPCOMING_SHORTCUTS: UpcomingShortcut[] = [
  { label: 'Nueva cita', icon: 'pi-plus-circle' },
  { label: 'Ajustes', icon: 'pi-cog' },
];

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, PushNotificationsCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home-page.component.html',
})
export class HomePageComponent {
  protected readonly session = inject(SessionService);
  protected readonly shortcuts = computed(() =>
    SHORTCUTS.filter((shortcut) => this.session.hasAnyPermission(shortcut.permissions ?? [])),
  );
  protected readonly upcoming = UPCOMING_SHORTCUTS;
  protected readonly today = TODAY_FORMAT.format(new Date());
}
