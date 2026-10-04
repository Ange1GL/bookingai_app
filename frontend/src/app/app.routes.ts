import { Routes } from '@angular/router';
import { APP_ROUTES } from '@/core/constants/routes';
import { authGuard } from '@/core/guard/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: APP_ROUTES.home },
  {
    path: APP_ROUTES.auth,
    loadChildren: () => import('./feature/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('@/shared/layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: APP_ROUTES.home,
        loadChildren: () => import('./feature/home/home.routes').then((m) => m.HOME_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: APP_ROUTES.home },
];
