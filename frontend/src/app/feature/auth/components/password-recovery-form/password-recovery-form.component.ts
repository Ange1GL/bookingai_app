import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { USERNAME_PATTERN } from '../../models/login-request.dto';

const USERNAME_MAX_LENGTH = 50;

interface PasswordRecoveryForm {
  username: FormControl<string>;
}

/** Asks only for the username: the admin verifies the person and issues the new access code. */
@Component({
  selector: 'app-password-recovery-form',
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, IconFieldModule, InputIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './password-recovery-form.component.html',
})
export class PasswordRecoveryFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loading = input(false);
  readonly submitted = output<string>();

  protected readonly usernameMaxLength = USERNAME_MAX_LENGTH;
  protected readonly form: FormGroup<PasswordRecoveryForm> = this.fb.group({
    username: this.fb.control('', [Validators.required, Validators.maxLength(USERNAME_MAX_LENGTH), Validators.pattern(USERNAME_PATTERN)]),
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.controls.username.value.trim());
  }
}
