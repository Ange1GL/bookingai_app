import { ChangeDetectionStrategy, Component, computed, inject, input, model, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import {
  CreateUserRequestDto,
  EMAIL_MAX_LENGTH,
  PERSON_NAME_MAX_LENGTH,
  USER_PHONE_LENGTH,
  USER_PHONE_PATTERN,
  USERNAME_MAX_LENGTH,
  USERNAME_MIN_LENGTH,
  USERNAME_PATTERN,
} from '../../models/user.dto';
import { UserForm } from '../../models/user-form.model';
import { RolesLookupService } from '../../service/roles-lookup.service';

@Component({
  selector: 'app-user-form-dialog',
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule, MultiSelectModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-form-dialog.component.html',
})
export class UserFormDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly rolesLookup = inject(RolesLookupService);

  readonly visible = model(false);
  readonly saving = input(false);
  readonly submitted = output<CreateUserRequestDto>();

  protected readonly usernameMaxLength = USERNAME_MAX_LENGTH;
  protected readonly nameMaxLength = PERSON_NAME_MAX_LENGTH;
  protected readonly emailMaxLength = EMAIL_MAX_LENGTH;
  protected readonly phoneLength = USER_PHONE_LENGTH;

  protected readonly form: FormGroup<UserForm> = this.fb.group({
    username: this.fb.control('', [
      Validators.required,
      Validators.minLength(USERNAME_MIN_LENGTH),
      Validators.maxLength(USERNAME_MAX_LENGTH),
      Validators.pattern(USERNAME_PATTERN),
    ]),
    name: this.fb.control('', [Validators.required, Validators.maxLength(PERSON_NAME_MAX_LENGTH)]),
    lastName: this.fb.control('', [Validators.required, Validators.maxLength(PERSON_NAME_MAX_LENGTH)]),
    email: this.fb.control('', [Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]),
    phone: this.fb.control('', [Validators.required, Validators.pattern(USER_PHONE_PATTERN)]),
    roleIds: this.fb.control<number[]>([], Validators.required),
  });

  /** Roles load each time the dialog opens, so a role created meanwhile shows up. */
  private readonly roles = rxResource({
    params: computed(() => (this.visible() ? true : undefined)),
    stream: () => this.rolesLookup.listActive(),
  });

  protected readonly roleOptions = computed(() => (this.roles.hasValue() ? this.roles.value() : []));
  protected readonly rolesLoading = this.roles.isLoading;
  protected readonly rolesFailed = computed(() => this.roles.error() !== undefined);

  protected reloadRoles(): void {
    this.roles.reload();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, name, lastName, email, phone, roleIds } = this.form.getRawValue();
    this.submitted.emit({
      username: username.trim(),
      name: name.trim(),
      lastName: lastName.trim(),
      email: email.trim() || null,
      phone: phone.trim(),
      roleIds,
    });
  }
}
