import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Permission } from '../security/permissions';
import { SessionService } from '../service/session.service';

const FALLBACK_URL = '/home';
const DENIED_MESSAGE = 'No tienes permiso para acceder a esta sección';

/**
 * Route guard requiring any of `required` (ADMIN_ALL always passes). Must run under `authGuard`, which restores the
 * session first. Denied users go home with a notice; the backend rejects the API calls anyway.
 */
export function permissionGuard(...required: Permission[]): CanActivateFn {
  return () => {
    const session = inject(SessionService);
    if (session.hasAnyPermission(required)) {
      return true;
    }
    inject(MessageService).add({ severity: 'warn', summary: 'Aviso', detail: DENIED_MESSAGE });
    return inject(Router).parseUrl(FALLBACK_URL);
  };
}
