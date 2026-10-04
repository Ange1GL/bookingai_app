import { Routes } from '@angular/router';

export const APPOINTMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./page/calendar-page/calendar-page.component').then((m) => m.CalendarPageComponent),
  },
];
