import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthUserDto } from '@/core/model/auth-user.dto';
import { AuthService } from '../../service/auth.service';
import { CreatePasswordPageComponent } from './create-password-page.component';

interface PageAccess {
  step(): 'verify' | 'password';
  notice(): string | null;
  cooldownSeconds(): number;
  verify(request: { username: string; code: string }): void;
  createPassword(password: string): void;
  goBack(): void;
}

const VERIFIED = { verificationToken: 'ticket', expiresAt: new Date(Date.now() + 600_000).toISOString() };
const REQUEST = { username: 'maria', code: '483920' };

describe('CreatePasswordPageComponent', () => {
  let fixture: ComponentFixture<CreatePasswordPageComponent>;
  let page: PageAccess;
  let authService: { verifyCode: ReturnType<typeof vi.fn>; createPassword: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authService = {
      verifyCode: vi.fn().mockReturnValue(of(VERIFIED)),
      createPassword: vi.fn().mockReturnValue(of({} as AuthUserDto)),
    };
    TestBed.configureTestingModule({
      imports: [CreatePasswordPageComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(CreatePasswordPageComponent);
    page = fixture.componentInstance as unknown as PageAccess;
    fixture.detectChanges();
  });

  it('starts on step 1 and only moves to step 2 once the backend verifies the code', () => {
    expect(page.step()).toBe('verify');

    page.verify(REQUEST);

    expect(authService.verifyCode).toHaveBeenCalledWith(REQUEST);
    expect(page.step()).toBe('password');
  });

  it('stays on step 1 when the backend rejects the code', () => {
    authService.verifyCode.mockReturnValue(throwError(() => new Error('rejected')));

    page.verify(REQUEST);

    expect(page.step()).toBe('verify');
  });

  it('cools down after three consecutive failures', () => {
    authService.verifyCode.mockReturnValue(throwError(() => new Error('rejected')));

    page.verify(REQUEST);
    page.verify(REQUEST);
    expect(page.cooldownSeconds()).toBe(0);
    page.verify(REQUEST);

    expect(page.cooldownSeconds()).toBeGreaterThan(0);
  });

  it('creates the password with the ticket from step 1 and goes home', () => {
    page.verify(REQUEST);

    page.createPassword('Mi-clave-2026');

    expect(authService.createPassword).toHaveBeenCalledWith({
      username: 'maria',
      code: '483920',
      verificationToken: 'ticket',
      newPassword: 'Mi-clave-2026',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });

  it('wipes the proof when going back, so step 2 cannot be submitted without verifying again', () => {
    page.verify(REQUEST);
    page.goBack();

    page.createPassword('Mi-clave-2026');

    expect(page.step()).toBe('verify');
    expect(authService.createPassword).not.toHaveBeenCalled();
  });

  it('returns to step 1 with a notice when creating the password fails', () => {
    authService.createPassword.mockReturnValue(throwError(() => new Error('rejected')));
    page.verify(REQUEST);

    page.createPassword('Mi-clave-2026');

    expect(page.step()).toBe('verify');
    expect(page.notice()).not.toBeNull();
  });
});
