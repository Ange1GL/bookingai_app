import { FormControl } from '@angular/forms';

export interface CustomerForm {
  name: FormControl<string>;
  phone: FormControl<string>;
}
