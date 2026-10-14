import { Routes } from '@angular/router';

export const LANDING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./page/landing-page/landing-page.component').then((m) => m.LandingPageComponent),
  },
];
