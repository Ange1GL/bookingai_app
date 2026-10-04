import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { LoginFormComponent } from '../../components/login-form/login-form.component';
import { LoginRequestDto } from '../../models/login-request.dto';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [LoginFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto mt-24 flex w-full max-w-sm flex-col gap-6 p-4">
      <h1 class="text-2xl font-semibold">Iniciar sesión</h1>
      <app-login-form [loading]="loading()" (submitted)="login($event)" />
      <p class="text-center text-sm">
        ¿No tienes cuenta?
        <a routerLink="/auth/register" class="text-primary">Regístrate</a>
      </p>
    </section>
  `,
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);

  protected login(request: LoginRequestDto): void {
    this.loading.set(true);
    this.authService
      .login(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/home'),
        error: () => undefined, // surfaced by errorInterceptor
      });
  }
}
