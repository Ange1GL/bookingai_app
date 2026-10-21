import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./page/login-page/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'password-recovery',
    loadComponent: () =>
      import('./page/password-recovery-page/password-recovery-page.component').then((m) => m.PasswordRecoveryPageComponent),
  },
  {
    path: 'create-password',
    loadComponent: () =>
      import('./page/create-password-page/create-password-page.component').then((m) => m.CreatePasswordPageComponent),
  },
];
