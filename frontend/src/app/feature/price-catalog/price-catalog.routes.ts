import { Routes } from '@angular/router';

export const PRICE_CATALOG_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./page/price-catalog-page/price-catalog-page.component').then((m) => m.PriceCatalogPageComponent),
  },
];
