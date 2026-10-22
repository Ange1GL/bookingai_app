import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthCardComponent } from '@/shared/components/auth-card/auth-card.component';
import { PasswordRecoveryFormComponent } from '../../components/password-recovery-form/password-recovery-form.component';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-password-recovery-page',
  imports: [AuthCardComponent, PasswordRecoveryFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './password-recovery-page.component.html',
})
export class PasswordRecoveryPageComponent {
  private readonly authService = inject(AuthService);

  protected readonly loading = signal(false);
  /** Neutral confirmation from the backend; it never reveals whether the user exists. */
  protected readonly confirmation = signal<string | null>(null);

  protected request(username: string): void {
    this.loading.set(true);
    this.authService
      .requestPasswordRecovery(username)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (accepted) => this.confirmation.set(accepted.message),
        error: () => undefined, // surfaced by errorInterceptor
      });
  }
}
