import { FormControl } from '@angular/forms';

export interface CreatePasswordForm {
  newPassword: FormControl<string>;
  confirmPassword: FormControl<string>;
}
