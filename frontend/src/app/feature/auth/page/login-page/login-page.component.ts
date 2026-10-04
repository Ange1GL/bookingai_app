import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthCardComponent } from '@/shared/components/auth-card/auth-card.component';
import { LoginFormComponent } from '../../components/login-form/login-form.component';
import { LoginRequestDto } from '../../models/login-request.dto';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [AuthCardComponent, LoginFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-card>
      <h2 class="m-0 text-center text-xl font-semibold">Iniciar sesión</h2>
      <app-login-form [loading]="loading()" (submitted)="login($event)" />
      <p class="m-0 text-center text-sm">
        ¿No tienes cuenta?
        <a routerLink="/auth/register" class="font-medium text-primary">Regístrate</a>
      </p>
    </app-auth-card>
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
