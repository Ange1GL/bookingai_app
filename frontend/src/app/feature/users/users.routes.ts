import { Routes } from '@angular/router';
import { permissionGuard } from '@/core/guard/permission.guard';
import { PERMISSIONS } from '@/core/security/permissions';

/** Every route needs USER_READ (ADMIN_ALL also passes); write actions are gated per button and by the backend. */
export const USERS_ROUTES: Routes = [
  {
    path: '',
    canActivate: [permissionGuard(PERMISSIONS.USER_READ)],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./page/users-page/users-page.component').then((m) => m.UsersPageComponent),
      },
      {
        path: 'recovery-requests',
        loadComponent: () =>
          import('./page/recovery-requests-page/recovery-requests-page.component').then((m) => m.RecoveryRequestsPageComponent),
      },
    ],
  },
];
