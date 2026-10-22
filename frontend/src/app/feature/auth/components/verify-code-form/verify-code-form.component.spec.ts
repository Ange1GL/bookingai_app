import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifyCodeRequestDto } from '../../models/verify-code.dto';
import { VerifyCodeFormComponent } from './verify-code-form.component';

interface FormAccess {
  form: { controls: Record<'username' | 'code', { setValue(value: string): void; value: string }> };
  submit(): void;
}

describe('VerifyCodeFormComponent', () => {
  let fixture: ComponentFixture<VerifyCodeFormComponent>;
  let component: FormAccess;
  let emitted: VerifyCodeRequestDto[];

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [VerifyCodeFormComponent] });
    fixture = TestBed.createComponent(VerifyCodeFormComponent);
    emitted = [];
    fixture.componentInstance.submitted.subscribe((request) => emitted.push(request));
    component = fixture.componentInstance as unknown as FormAccess;
    fixture.detectChanges();
  });

  function fill(username: string, code: string): void {
    component.form.controls.username.setValue(username);
    component.form.controls.code.setValue(code);
  }

  it('does not emit while the form is empty', () => {
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('emits the username and the code when both are valid', () => {
    fill('maria.lopez', '483920');

    component.submit();

    expect(emitted).toEqual([{ username: 'maria.lopez', code: '483920' }]);
  });

  it('rejects a code that is not exactly six digits', () => {
    fill('maria.lopez', '1234');
    component.submit();
    fill('maria.lopez', '12345a');
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('rejects usernames with spaces or longer than the limit', () => {
    fill('maria lopez', '483920');
    component.submit();
    fill('x'.repeat(51), '483920');
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('blocks submitting during the cooldown and while loading', () => {
    fill('maria.lopez', '483920');
    fixture.componentRef.setInput('cooldownSeconds', 20);
    component.submit();
    fixture.componentRef.setInput('cooldownSeconds', 0);
    fixture.componentRef.setInput('loading', true);
    component.submit();

    expect(emitted).toEqual([]);
  });

  it('clears the code but keeps the username', () => {
    fill('maria.lopez', '483920');

    fixture.componentInstance.clearCode();

    expect(component.form.controls.code.value).toBe('');
    expect(component.form.controls.username.value).toBe('maria.lopez');
  });
});
