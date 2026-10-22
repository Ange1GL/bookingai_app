import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { PasswordModule } from 'primeng/password';
import { fieldsMatchValidator } from '@/shared/validators/fields-match.validator';
import {
  PASSWORD_MIN_LENGTH,
  passwordPolicyValidator,
} from '@/shared/validators/password-policy.validator';
import { CreatePasswordForm } from '../../models/create-password-form.model';

interface PasswordRequirement {
  label: string;
  met: boolean;
}

const LETTER = /\p{L}/u;
const DIGIT = /\p{Nd}/u;

/** Step 2: the new password. It only exists after step 1 verified the code, so it never asks for credentials. */
@Component({
  selector: 'app-create-password-form',
  imports: [ReactiveFormsModule, PasswordModule, ButtonModule, IconFieldModule, InputIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-password-form.component.html',
})
export class CreatePasswordFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loading = input(false);
  readonly submitted = output<string>();
  readonly back = output<void>();

  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected readonly form: FormGroup<CreatePasswordForm> = this.fb.group(
    {
      newPassword: this.fb.control('', [Validators.required, passwordPolicyValidator]),
      confirmPassword: this.fb.control('', Validators.required),
    },
    { validators: fieldsMatchValidator('newPassword', 'confirmPassword') },
  );

  private readonly password = toSignal(this.form.controls.newPassword.valueChanges, { initialValue: '' });
  private readonly confirmation = toSignal(this.form.controls.confirmPassword.valueChanges, { initialValue: '' });

  protected readonly requirements = computed<PasswordRequirement[]>(() => {
    const value = this.password();
    return [
      { label: `Al menos ${PASSWORD_MIN_LENGTH} caracteres`, met: value.length >= PASSWORD_MIN_LENGTH },
      { label: 'Una letra', met: LETTER.test(value) },
      { label: 'Un número', met: DIGIT.test(value) },
      { label: 'Las dos contraseñas coinciden', met: value !== '' && value === this.confirmation() },
    ];
  });

  protected submit(): void {
    if (this.loading()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.controls.newPassword.value);
  }
}
