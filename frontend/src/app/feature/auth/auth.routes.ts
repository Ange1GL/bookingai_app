import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./page/login-page/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./page/register-page/register-page.component').then((m) => m.RegisterPageComponent),
  },
];
