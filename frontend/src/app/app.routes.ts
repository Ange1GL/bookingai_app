import { Routes } from '@angular/router';
import { authGuard } from '@/core/guard/auth.guard';
import { permissionGuard } from '@/core/guard/permission.guard';
import { PERMISSIONS } from '@/core/security/permissions';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () => import('./feature/landing/landing.routes').then((m) => m.LANDING_ROUTES),
  },
  {
    path: 'terminos',
    loadComponent: () => import('./feature/landing/page/terms-page/terms-page.component').then((m) => m.TermsPageComponent),
  },
  {
    path: 'privacidad',
    loadComponent: () =>
      import('./feature/landing/page/privacy-page/privacy-page.component').then((m) => m.PrivacyPageComponent),
  },
  {
    path: 'auth',
    loadChildren: () => import('./feature/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('@/shared/layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: 'appointments',
        loadChildren: () => import('./feature/appointments/appointments.routes').then((m) => m.APPOINTMENTS_ROUTES),
      },
      {
        path: 'customers',
        loadChildren: () => import('./feature/customers/customers.routes').then((m) => m.CUSTOMERS_ROUTES),
      },
      {
        path: 'price-catalog',
        loadChildren: () => import('./feature/price-catalog/price-catalog.routes').then((m) => m.PRICE_CATALOG_ROUTES),
      },
      {
        path: 'assistant',
        loadChildren: () => import('./feature/assistant/assistant.routes').then((m) => m.ASSISTANT_ROUTES),
      },
      {
        path: 'users',
        canActivate: [permissionGuard(PERMISSIONS.USER_READ)],
        loadChildren: () => import('./feature/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'home',
        loadChildren: () => import('./feature/home/home.routes').then((m) => m.HOME_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: 'home' },
];
