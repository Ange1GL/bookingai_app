import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthCardComponent } from '@/shared/components/auth-card/auth-card.component';
import { CreatePasswordFormComponent } from '../../components/create-password-form/create-password-form.component';
import { VerifyCodeFormComponent } from '../../components/verify-code-form/verify-code-form.component';
import { VerifyCodeRequestDto } from '../../models/verify-code.dto';
import { AuthService } from '../../service/auth.service';

type Step = 'verify' | 'password';

/** What step 1 proved. Lives only in memory (never storage or the URL) and is wiped when the flow resets. */
interface VerifiedCode {
  username: string;
  code: string;
  verificationToken: string;
}

const FAILURES_BEFORE_COOLDOWN = 3;
const COOLDOWN_SECONDS = 30;
const MS_PER_SECOND = 1000;
const EXPIRED_NOTICE = 'La verificación expiró. Vuelve a ingresar tu código.';
const PASSWORD_FAILED_NOTICE = 'No se pudo crear la contraseña. Verifica tu código de nuevo.';

@Component({
  selector: 'app-create-password-page',
  imports: [AuthCardComponent, VerifyCodeFormComponent, CreatePasswordFormComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-password-page.component.html',
})
export class CreatePasswordPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly verifyForm = viewChild(VerifyCodeFormComponent);
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;
  private cooldownTimer: ReturnType<typeof setInterval> | undefined;
  private consecutiveFailures = 0;

  protected readonly step = signal<Step>('verify');
  protected readonly loading = signal(false);
  protected readonly cooldownSeconds = signal(0);
  protected readonly notice = signal<string | null>(null);
  private readonly verified = signal<VerifiedCode | null>(null);

  protected readonly stepNumber = computed(() => (this.step() === 'verify' ? 1 : 2));

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      this.wipe();
      clearInterval(this.cooldownTimer);
    });
  }

  protected verify(request: VerifyCodeRequestDto): void {
    this.loading.set(true);
    this.notice.set(null);
    this.authService
      .verifyCode(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ verificationToken, expiresAt }) => {
          this.consecutiveFailures = 0;
          this.verified.set({ username: request.username, code: request.code, verificationToken });
          this.step.set('password');
          this.scheduleExpiry(expiresAt);
        },
        // errorInterceptor shows the (generic) message; a rejected code is cleared and repeated misses cool down.
        error: () => {
          this.verifyForm()?.clearCode();
          this.registerFailure();
        },
      });
  }

  protected createPassword(newPassword: string): void {
    const proof = this.verified();
    if (proof === null) {
      this.restart(EXPIRED_NOTICE);
      return;
    }
    this.loading.set(true);
    this.authService
      .createPassword({ ...proof, newPassword })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.wipe();
          void this.router.navigateByUrl('/home');
        },
        error: () => this.restart(PASSWORD_FAILED_NOTICE),
      });
  }

  protected goBack(): void {
    this.restart(null);
  }

  private restart(notice: string | null): void {
    this.wipe();
    this.step.set('verify');
    this.notice.set(notice);
  }

  private wipe(): void {
    clearTimeout(this.expiryTimer);
    this.verified.set(null);
  }

  private scheduleExpiry(expiresAt: string): void {
    clearTimeout(this.expiryTimer);
    const remainingMs = Math.max(Date.parse(expiresAt) - Date.now(), 0);
    this.expiryTimer = setTimeout(() => this.restart(EXPIRED_NOTICE), remainingMs);
  }

  private registerFailure(): void {
    this.consecutiveFailures++;
    if (this.consecutiveFailures >= FAILURES_BEFORE_COOLDOWN) {
      this.startCooldown();
    }
  }

  private startCooldown(): void {
    clearInterval(this.cooldownTimer);
    this.cooldownSeconds.set(COOLDOWN_SECONDS);
    this.cooldownTimer = setInterval(() => {
      this.cooldownSeconds.update((seconds) => seconds - 1);
      if (this.cooldownSeconds() <= 0) {
        clearInterval(this.cooldownTimer);
      }
    }, MS_PER_SECOND);
  }
}
