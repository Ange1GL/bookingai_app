import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthCardComponent } from '@/shared/components/auth-card/auth-card.component';
import { RegisterFormComponent } from '../../components/register-form/register-form.component';
import { RegisterRequestDto } from '../../models/register-request.dto';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-register-page',
  imports: [AuthCardComponent, RegisterFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-card>
      <h2 class="m-0 text-center text-xl font-semibold">Crear cuenta</h2>
      <app-register-form [loading]="loading()" (submitted)="register($event)" />
      <p class="m-0 text-center text-sm">
        ¿Ya tienes cuenta?
        <a routerLink="/auth/login" class="font-medium text-primary">Inicia sesión</a>
      </p>
    </app-auth-card>
  `,
})
export class RegisterPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);

  protected register(request: RegisterRequestDto): void {
    this.loading.set(true);
    this.authService
      .register(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/home'),
        error: () => undefined, // surfaced by errorInterceptor
      });
  }
}
