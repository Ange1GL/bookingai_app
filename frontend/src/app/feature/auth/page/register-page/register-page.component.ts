import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { APP_ROUTES, HOME_URL } from '@/core/constants/routes';
import { RegisterFormComponent } from '../../components/register-form/register-form.component';
import { RegisterRequestDto } from '../../models/register-request.dto';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-register-page',
  imports: [RegisterFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto mt-16 flex w-full max-w-sm flex-col gap-6 p-4">
      <h1 class="text-2xl font-semibold">Crear cuenta</h1>
      <app-register-form [loading]="loading()" (submitted)="register($event)" />
      <p class="text-center text-sm">
        ¿Ya tienes cuenta?
        <a [routerLink]="loginLink" class="text-primary">Inicia sesión</a>
      </p>
    </section>
  `,
})
export class RegisterPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly loginLink = `/${APP_ROUTES.auth}/${APP_ROUTES.login}`;

  protected register(request: RegisterRequestDto): void {
    this.loading.set(true);
    this.authService
      .register(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl(HOME_URL),
        error: () => undefined, // surfaced by errorInterceptor
      });
  }
}
