import { FormControl } from '@angular/forms';

export interface PriceServiceForm {
  label: FormControl<string>;
  price: FormControl<number | null>;
}
