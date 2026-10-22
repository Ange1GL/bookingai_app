import { TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { ClipboardService } from '@/core/service/clipboard.service';
import { CopyButtonComponent } from './copy-button.component';

describe('CopyButtonComponent', () => {
  const setup = (copyResult: boolean) => {
    const copy = vi.fn().mockResolvedValue(copyResult);
    TestBed.configureTestingModule({
      providers: [{ provide: ClipboardService, useValue: { copy } }, MessageService],
    });
    const fixture = TestBed.createComponent(CopyButtonComponent);
    fixture.componentRef.setInput('value', 'usuario: ana\ncodigoAcceso: 483920');
    fixture.detectChanges();
    return { fixture, copy, messages: TestBed.inject(MessageService) };
  };

  it('copies the value and shows the confirmation', async () => {
    const { fixture, copy } = setup(true);

    fixture.nativeElement.querySelector('button').click();

    expect(copy).toHaveBeenCalledWith('usuario: ana\ncodigoAcceso: 483920');
    await vi.waitFor(() => {
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Copiado');
    });
  });

  it('warns when the clipboard is unavailable', async () => {
    const { fixture, messages } = setup(false);
    const add = vi.spyOn(messages, 'add');

    fixture.nativeElement.querySelector('button').click();
    await fixture.whenStable();

    expect(add).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).not.toContain('Copiado');
  });
});
