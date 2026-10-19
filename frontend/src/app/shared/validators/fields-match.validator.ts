import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Group-level validator: sets `fieldsMismatch` on the group when the two named controls differ.
 * It stays silent until both have a value so "required" is the only message on an untouched confirmation.
 */
export function fieldsMatchValidator(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const value: unknown = group.get(field)?.value;
    const confirmation: unknown = group.get(confirmField)?.value;
    if (!value || !confirmation) {
      return null;
    }
    return value === confirmation ? null : { fieldsMismatch: true };
  };
}
