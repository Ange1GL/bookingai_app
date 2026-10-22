import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreatePasswordFormComponent } from './create-password-form.component';

interface FormAccess {
  form: {
    controls: Record<'newPassword' | 'confirmPassword', { setValue(value: string): void }>;
    hasError(error: string): boolean;
  };
  submit(): void;
}

describe('CreatePasswordFormComponent', () => {
  let fixture: ComponentFixture<CreatePasswordFormComponent>;
  let component: FormAccess;
  let emitted: string[];

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CreatePasswordFormComponent] });
    fixture = TestBed.createComponent(CreatePasswordFormComponent);
    emitted = [];
    fixture.componentInstance.submitted.subscribe((password) => emitted.push(password));
    component = fixture.componentInstance as unknown as FormAccess;
    fixture.detectChanges();
  });

  function fill(newPassword: string, confirmPassword: string): void {
    component.form.controls.newPassword.setValue(newPassword);
    component.form.controls.confirmPassword.setValue(confirmPassword);
  }

  it('does not emit while the form is empty', () => {
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('emits only the password (never the confirmation) when valid', () => {
    fill('Mi-clave-2026', 'Mi-clave-2026');

    component.submit();

    expect(emitted).toEqual(['Mi-clave-2026']);
  });

  it('rejects passwords that do not match', () => {
    fill('Mi-clave-2026', 'Otra-clave-2026');

    expect(component.form.hasError('fieldsMismatch')).toBe(true);
    component.submit();
    expect(emitted).toEqual([]);
  });

  it('rejects a password that breaks the policy', () => {
    fill('corta1', 'corta1');

    component.submit();

    expect(emitted).toEqual([]);
  });

  it('does not emit while a request is in flight', () => {
    fixture.componentRef.setInput('loading', true);
    fill('Mi-clave-2026', 'Mi-clave-2026');

    component.submit();

    expect(emitted).toEqual([]);
  });

  it('emits back when the user returns to step 1', () => {
    let backs = 0;
    fixture.componentInstance.back.subscribe(() => backs++);

    fixture.componentInstance.back.emit();

    expect(backs).toBe(1);
  });
});
