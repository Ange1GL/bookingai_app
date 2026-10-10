import { Routes } from '@angular/router';

export const CUSTOMERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./page/customers-page/customers-page.component').then((m) => m.CustomersPageComponent),
  },
];
