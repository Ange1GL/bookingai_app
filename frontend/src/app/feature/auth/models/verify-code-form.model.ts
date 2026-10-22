import { FormControl } from '@angular/forms';

export interface VerifyCodeForm {
  username: FormControl<string>;
  code: FormControl<string>;
}
