import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreatePasswordRequestDto } from '../../models/create-password-request.dto';
import { CreatePasswordFormComponent } from './create-password-form.component';

interface FormAccess {
  form: {
    controls: Record<'username' | 'code' | 'newPassword' | 'confirmPassword', { setValue(value: string): void }>;
    valid: boolean;
    hasError(error: string): boolean;
  };
  submit(): void;
}

describe('CreatePasswordFormComponent', () => {
  let fixture: ComponentFixture<CreatePasswordFormComponent>;
  let component: FormAccess;
  let emitted: CreatePasswordRequestDto[];

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CreatePasswordFormComponent] });
    fixture = TestBed.createComponent(CreatePasswordFormComponent);
    emitted = [];
    fixture.componentInstance.submitted.subscribe((request) => emitted.push(request));
    component = fixture.componentInstance as unknown as FormAccess;
    fixture.detectChanges();
  });

  function fill(values: Partial<Record<'username' | 'code' | 'newPassword' | 'confirmPassword', string>>): void {
    const complete = {
      username: 'maria.lopez',
      code: '483920',
      newPassword: 'Mi-clave-2026',
      confirmPassword: 'Mi-clave-2026',
      ...values,
    };
    component.form.controls.username.setValue(complete.username);
    component.form.controls.code.setValue(complete.code);
    component.form.controls.newPassword.setValue(complete.newPassword);
    component.form.controls.confirmPassword.setValue(complete.confirmPassword);
  }

  it('does not emit while the form is empty', () => {
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('emits username, code and password (without the confirmation) when valid', () => {
    fill({});

    component.submit();

    expect(emitted).toEqual([{ username: 'maria.lopez', code: '483920', newPassword: 'Mi-clave-2026' }]);
  });

  it('rejects a code that is not exactly six digits', () => {
    fill({ code: '1234' });
    component.submit();
    fill({ code: '12345a' });
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('rejects passwords that do not match', () => {
    fill({ confirmPassword: 'Otra-clave-2026' });

    expect(component.form.hasError('fieldsMismatch')).toBe(true);
    component.submit();
    expect(emitted).toEqual([]);
  });

  it('rejects a password that breaks the policy', () => {
    fill({ newPassword: 'corta1', confirmPassword: 'corta1' });

    component.submit();

    expect(emitted).toEqual([]);
  });

  it('rejects usernames with spaces', () => {
    fill({ username: 'maria lopez' });

    component.submit();

    expect(emitted).toEqual([]);
  });
});
