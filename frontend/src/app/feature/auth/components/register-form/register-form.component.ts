import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { PASSWORD_MIN_LENGTH, RegisterRequestDto } from '../../models/register-request.dto';

@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule, InputTextModule, PasswordModule, ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register-form.component.html',
})
export class RegisterFormComponent {
  readonly loading = input(false);
  readonly submitted = output<RegisterRequestDto>();

  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', Validators.required],
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.getRawValue());
  }
}
