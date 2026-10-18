import { FormControl } from '@angular/forms';
import { CustomerOptionDto, ServiceOptionDto } from './booking-options.model';

export interface AppointmentForm {
  customer: FormControl<CustomerOptionDto | null>;
  service: FormControl<ServiceOptionDto | null>;
  start: FormControl<Date | null>;
  end: FormControl<Date | null>;
}

export interface AppointmentFormValue {
  customer: CustomerOptionDto | null;
  service: ServiceOptionDto | null;
  start: Date | null;
  end: Date | null;
}
