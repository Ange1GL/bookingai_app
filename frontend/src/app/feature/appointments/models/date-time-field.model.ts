import { FormControl } from '@angular/forms';

/** Inner controls of `app-date-time-field`: the day and the typed `HH:mm` text. */
export interface DateTimeParts {
  date: FormControl<Date | null>;
  time: FormControl<string>;
}
