import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputOtpModule } from 'primeng/inputotp';
import { InputTextModule } from 'primeng/inputtext';
import { OTP_LENGTH, OTP_PATTERN } from '../../models/create-password-request.dto';
import { USERNAME_PATTERN } from '../../models/login-request.dto';
import { VerifyCodeForm } from '../../models/verify-code-form.model';
import { USERNAME_MAX_LENGTH, VerifyCodeRequestDto } from '../../models/verify-code.dto';

/** Step 1: username + one-time code. Nothing moves forward until the backend confirms both. */
@Component({
  selector: 'app-verify-code-form',
  imports: [ReactiveFormsModule, InputTextModule, InputOtpModule, ButtonModule, IconFieldModule, InputIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './verify-code-form.component.html',
})
export class VerifyCodeFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loading = input(false);
  /** Seconds left of the client-side cooldown after repeated failures; 0 means the form is usable. */
  readonly cooldownSeconds = input(0);
  readonly submitted = output<VerifyCodeRequestDto>();

  protected readonly otpLength = OTP_LENGTH;
  protected readonly usernameMaxLength = USERNAME_MAX_LENGTH;

  protected readonly form: FormGroup<VerifyCodeForm> = this.fb.group({
    username: this.fb.control('', [
      Validators.required,
      Validators.maxLength(USERNAME_MAX_LENGTH),
      Validators.pattern(USERNAME_PATTERN),
    ]),
    code: this.fb.control('', [Validators.required, Validators.pattern(OTP_PATTERN)]),
  });

  /** A rejected code is never worth keeping on screen. */
  clearCode(): void {
    this.form.controls.code.reset('');
  }

  protected submit(): void {
    if (this.loading() || this.cooldownSeconds() > 0) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, code } = this.form.getRawValue();
    this.submitted.emit({ username: username.trim(), code });
  }
}
