import { FormControl } from '@angular/forms';
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH, passwordPolicyValidator } from './password-policy.validator';

function validate(value: string) {
  return passwordPolicyValidator(new FormControl<string>(value, { nonNullable: true }));
}

describe('passwordPolicyValidator', () => {
  it('accepts a password with letters and digits of valid length', () => {
    expect(validate('abcdefghi1')).toBeNull();
    expect(validate('Contraseña-2026')).toBeNull();
  });

  it('leaves empty values to the required validator', () => {
    expect(validate('')).toBeNull();
  });

  it('rejects passwords shorter than the minimum', () => {
    expect(validate('a'.repeat(PASSWORD_MIN_LENGTH - 2) + '1')).toEqual({ passwordTooShort: true });
  });

  it('rejects passwords longer than bcrypt can use', () => {
    const tooLong = 'a'.repeat(PASSWORD_MAX_LENGTH) + '1';
    expect(validate(tooLong)).toEqual({ passwordTooLong: true });
  });

  it('requires at least one letter and one digit', () => {
    expect(validate('abcdefghijk')).toEqual({ passwordNeedsLetterAndDigit: true });
    expect(validate('12345678901')).toEqual({ passwordNeedsLetterAndDigit: true });
    expect(validate('!!!!!!!!!!!!')).toEqual({ passwordNeedsLetterAndDigit: true });
  });

  it('reports every broken rule at once', () => {
    expect(validate('abc')).toEqual({ passwordTooShort: true, passwordNeedsLetterAndDigit: true });
  });
});
