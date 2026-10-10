import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessionService } from '@/core/service/session.service';

const TODAY_FORMAT = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long' });

interface Shortcut {
  label: string;
  description: string;
  icon: string;
  path: string;
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
];

const UPCOMING_SHORTCUTS: UpcomingShortcut[] = [
  { label: 'Nueva cita', icon: 'pi-plus-circle' },
  { label: 'Ajustes', icon: 'pi-cog' },
];

@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home-page.component.html',
})
export class HomePageComponent {
  protected readonly session = inject(SessionService);
  protected readonly shortcuts = SHORTCUTS;
  protected readonly upcoming = UPCOMING_SHORTCUTS;
  protected readonly today = TODAY_FORMAT.format(new Date());
}
