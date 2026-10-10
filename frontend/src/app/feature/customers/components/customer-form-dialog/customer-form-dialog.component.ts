import { ChangeDetectionStrategy, Component, inject, input, model, output } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import {
  CreateCustomerRequestDto,
  NAME_MAX_LENGTH,
  PHONE_MAX_LENGTH,
  PHONE_PATTERN,
} from '../../models/customer.dto';
import { CustomerForm } from '../../models/customer-form.model';

@Component({
  selector: 'app-customer-form-dialog',
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-form-dialog.component.html',
})
export class CustomerFormDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly visible = model(false);
  readonly saving = input(false);
  readonly submitted = output<CreateCustomerRequestDto>();

  protected readonly nameMaxLength = NAME_MAX_LENGTH;
  protected readonly phoneMaxLength = PHONE_MAX_LENGTH;
  protected readonly form: FormGroup<CustomerForm> = this.fb.group({
    name: this.fb.control('', [Validators.required, Validators.maxLength(NAME_MAX_LENGTH)]),
    phone: this.fb.control('', [Validators.required, Validators.maxLength(PHONE_MAX_LENGTH), Validators.pattern(PHONE_PATTERN)]),
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, phone } = this.form.getRawValue();
    this.submitted.emit({ name: name.trim(), phone: phone.trim() });
  }
}
