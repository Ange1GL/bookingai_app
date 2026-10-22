import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { MessageService } from 'primeng/api';
import { PERMISSIONS } from '../security/permissions';
import { SessionService } from '../service/session.service';
import { permissionGuard } from './permission.guard';

const user = (permissions: string[]) => ({ userId: 1, username: 'ana', email: 'ana@test.com', roles: [], permissions });

describe('permissionGuard', () => {
  const run = () =>
    TestBed.runInInjectionContext(() =>
      permissionGuard(PERMISSIONS.USER_READ)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([]), MessageService] });
  });

  it('allows a user holding the permission', () => {
    TestBed.inject(SessionService).setUser(user([PERMISSIONS.USER_READ]));
    expect(run()).toBe(true);
  });

  it('allows ADMIN_ALL', () => {
    TestBed.inject(SessionService).setUser(user([PERMISSIONS.ADMIN_ALL]));
    expect(run()).toBe(true);
  });

  it('redirects home and warns when the permission is missing', () => {
    TestBed.inject(SessionService).setUser(user([PERMISSIONS.ROLE_READ]));
    const add = vi.spyOn(TestBed.inject(MessageService), 'add');

    const result = run();

    expect((result as UrlTree).toString()).toBe(TestBed.inject(Router).parseUrl('/home').toString());
    expect(add).toHaveBeenCalledOnce();
  });

  it('denies when there is no session', () => {
    expect(run()).toBeInstanceOf(UrlTree);
  });
});
