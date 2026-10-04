import { Routes } from '@angular/router';
import { authGuard } from '@/core/guard/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
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
        path: 'home',
        loadChildren: () => import('./feature/home/home.routes').then((m) => m.HOME_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: 'home' },
];
