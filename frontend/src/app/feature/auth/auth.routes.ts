import { Routes } from '@angular/router';
import { APP_ROUTES } from '@/core/constants/routes';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: APP_ROUTES.login },
  {
    path: APP_ROUTES.login,
    loadComponent: () => import('./page/login-page/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: APP_ROUTES.register,
    loadComponent: () => import('./page/register-page/register-page.component').then((m) => m.RegisterPageComponent),
  },
];
