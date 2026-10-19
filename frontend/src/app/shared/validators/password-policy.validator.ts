import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Mirrors the backend PasswordPolicy: 10-72 characters with at least one letter and one digit. */
export const PASSWORD_MIN_LENGTH = 10;
export const PASSWORD_MAX_LENGTH = 72;

const LETTER = /\p{L}/u;
const DIGIT = /\p{Nd}/u;

/** Empty values are left to `Validators.required` so the field shows a single, specific message. */
export const passwordPolicyValidator: ValidatorFn = (control: AbstractControl<string>): ValidationErrors | null => {
  const value = control.value;
  if (!value) {
    return null;
  }
  const errors: ValidationErrors = {};
  if (value.length < PASSWORD_MIN_LENGTH) {
    errors['passwordTooShort'] = true;
  }
  if (value.length > PASSWORD_MAX_LENGTH) {
    errors['passwordTooLong'] = true;
  }
  if (!LETTER.test(value) || !DIGIT.test(value)) {
    errors['passwordNeedsLetterAndDigit'] = true;
  }
  return Object.keys(errors).length > 0 ? errors : null;
};
