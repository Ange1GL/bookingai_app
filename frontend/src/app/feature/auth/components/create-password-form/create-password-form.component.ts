import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputOtpModule } from 'primeng/inputotp';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { fieldsMatchValidator } from '@/shared/validators/fields-match.validator';
import {
  PASSWORD_MIN_LENGTH,
  passwordPolicyValidator,
} from '@/shared/validators/password-policy.validator';
import { CreatePasswordForm } from '../../models/create-password-form.model';
import { CreatePasswordRequestDto, OTP_LENGTH, OTP_PATTERN } from '../../models/create-password-request.dto';
import { USERNAME_PATTERN } from '../../models/login-request.dto';

@Component({
  selector: 'app-create-password-form',
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    InputOtpModule,
    PasswordModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-password-form.component.html',
})
export class CreatePasswordFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loading = input(false);
  readonly submitted = output<CreatePasswordRequestDto>();

  protected readonly otpLength = OTP_LENGTH;
  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected readonly form: FormGroup<CreatePasswordForm> = this.fb.group(
    {
      username: this.fb.control('', [Validators.required, Validators.pattern(USERNAME_PATTERN)]),
      code: this.fb.control('', [Validators.required, Validators.pattern(OTP_PATTERN)]),
      newPassword: this.fb.control('', [Validators.required, passwordPolicyValidator]),
      confirmPassword: this.fb.control('', Validators.required),
    },
    { validators: fieldsMatchValidator('newPassword', 'confirmPassword') },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, code, newPassword } = this.form.getRawValue();
    this.submitted.emit({ username, code, newPassword });
  }
}
