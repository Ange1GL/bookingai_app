import { FormControl } from '@angular/forms';

export interface CreatePasswordForm {
  username: FormControl<string>;
  code: FormControl<string>;
  newPassword: FormControl<string>;
  confirmPassword: FormControl<string>;
}
