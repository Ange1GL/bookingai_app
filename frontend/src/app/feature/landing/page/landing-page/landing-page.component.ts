import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CONTACT_EMAIL, LOGIN_PATH, PRIVACY_PATH } from '../../data/contact';
import { PRIVACY_HIGHLIGHTS } from '../../data/privacy-policy';

const CONTACT_SUBJECT = 'Quiero adquirir aglacita';

interface Benefit {
  title: string;
  description: string;
  icon: string;
}

const BENEFITS: Benefit[] = [
  {
    title: 'Agenda sin enredos',
    description: 'Calendario mensual y semanal para ver todas tus citas de un vistazo.',
    icon: 'pi-calendar',
  },
  {
    title: 'Asistente de AI',
    description: 'Agenda, mueve o cancela citas conversando, sin llenar formularios.',
    icon: 'pi-sparkles',
  },
  {
    title: 'Clientes bajo control',
    description: 'Busca a tus clientes y bloquea a quienes no quieras volver a atender.',
    icon: 'pi-users',
  },
  {
    title: 'Tus precios, claros',
    description: 'Administra tu catálogo de servicios y cuánto cobras por cada uno.',
    icon: 'pi-tag',
  },
];

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landing-page.component.html',
})
export class LandingPageComponent {
  protected readonly benefits = BENEFITS;
  protected readonly highlights = PRIVACY_HIGHLIGHTS;
  protected readonly loginPath = LOGIN_PATH;
  protected readonly privacyPath = PRIVACY_PATH;
  protected readonly contactEmail = CONTACT_EMAIL;
  protected readonly contactHref = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(CONTACT_SUBJECT)}`;
  protected readonly year = new Date().getFullYear();
}
