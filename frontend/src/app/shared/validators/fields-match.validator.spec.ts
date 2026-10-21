import { FormControl, FormGroup } from '@angular/forms';
import { fieldsMatchValidator } from './fields-match.validator';

function group(password: string, confirmation: string): FormGroup {
  return new FormGroup(
    { password: new FormControl(password), confirm: new FormControl(confirmation) },
    { validators: fieldsMatchValidator('password', 'confirm') },
  );
}

describe('fieldsMatchValidator', () => {
  it('passes when both fields are equal', () => {
    expect(group('abc12345678', 'abc12345678').errors).toBeNull();
  });

  it('flags a mismatch', () => {
    expect(group('abc12345678', 'abc12345679').errors).toEqual({ fieldsMismatch: true });
  });

  it('stays silent while the confirmation is empty', () => {
    expect(group('abc12345678', '').errors).toBeNull();
  });
});
