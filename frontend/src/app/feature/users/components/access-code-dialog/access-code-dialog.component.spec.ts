import { TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { UserOneTimeCodeDto } from '../../models/user.dto';
import { AccessCodeDialogComponent } from './access-code-dialog.component';

const USER = {
  id: 7,
  username: 'maria.lopez',
  name: 'María',
  lastName: 'López',
  email: null,
  phone: '5512345678',
  active: true,
  passwordSet: false,
  roles: [],
};

describe('AccessCodeDialogComponent', () => {
  const render = (result: UserOneTimeCodeDto) => {
    TestBed.configureTestingModule({ providers: [MessageService] });
    const fixture = TestBed.createComponent(AccessCodeDialogComponent);
    fixture.componentRef.setInput('result', result);
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();
    return document.body.textContent ?? '';
  };

  it('shows the code and the share message for manual delivery', () => {
    const text = render({ user: USER, codeSent: false, oneTimeCode: '483920', shareMessage: 'usuario: maria.lopez\ncodigoAcceso: 483920' });

    expect(text).toContain('483920');
    expect(document.querySelector('[data-testid="share-message"]')?.textContent).toContain('codigoAcceso: 483920');
    expect(document.querySelectorAll('app-copy-button').length).toBe(1);
  });

  it('only confirms the delivery when WhatsApp sent the code', () => {
    const text = render({ user: USER, codeSent: true });

    expect(text).toContain('se envió por WhatsApp');
    expect(document.querySelector('app-copy-button')).toBeNull();
  });
});
