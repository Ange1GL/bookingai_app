import { FormControl } from '@angular/forms';

export interface UserForm {
  username: FormControl<string>;
  name: FormControl<string>;
  lastName: FormControl<string>;
  email: FormControl<string>;
  phone: FormControl<string>;
  roleIds: FormControl<number[]>;
}
