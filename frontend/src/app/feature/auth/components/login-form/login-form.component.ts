import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { LoginForm } from '../../models/login-form.model';
import { LoginRequestDto, USERNAME_PATTERN } from '../../models/login-request.dto';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, InputTextModule, PasswordModule, ButtonModule, IconFieldModule, InputIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-form.component.html',
})
export class LoginFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loading = input(false);
  readonly submitted = output<LoginRequestDto>();

  protected readonly form: FormGroup<LoginForm> = this.fb.group({
    username: this.fb.control('', [Validators.required, Validators.pattern(USERNAME_PATTERN)]),
    password: this.fb.control('', Validators.required),
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const credentials: LoginRequestDto = this.form.getRawValue();
    this.submitted.emit(credentials);
  }
}
