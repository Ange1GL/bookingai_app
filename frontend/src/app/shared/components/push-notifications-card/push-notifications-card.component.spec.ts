import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { PushStatus, PushTestResponseDto } from '@/core/model/push.model';
import { PushNotificationsService } from '@/core/service/push-notifications.service';
import { PushNotificationsCardComponent } from './push-notifications-card.component';

describe('PushNotificationsCardComponent', () => {
  let fixture: ComponentFixture<PushNotificationsCardComponent>;
  let status: ReturnType<typeof signal<PushStatus>>;
  let service: {
    status: ReturnType<typeof signal<PushStatus>>;
    enable: ReturnType<typeof vi.fn<() => Observable<void>>>;
    disable: ReturnType<typeof vi.fn<() => Observable<void>>>;
    sendTest: ReturnType<typeof vi.fn<() => Observable<PushTestResponseDto>>>;
    syncExisting: ReturnType<typeof vi.fn<() => Observable<void>>>;
  };

  function create(initial: PushStatus): HTMLElement {
    status.set(initial);
    fixture = TestBed.createComponent(PushNotificationsCardComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function clickButton(el: HTMLElement, label: string): void {
    const button = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(label));
    expect(button, `button "${label}"`).toBeTruthy();
    button?.click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    status = signal<PushStatus>('off');
    service = {
      status,
      enable: vi.fn(() => of(undefined)),
      disable: vi.fn(() => of(undefined)),
      sendTest: vi.fn(() => of({ delivered: 1 })),
      syncExisting: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: PushNotificationsService, useValue: service }] });
  });

  it('renders nothing where push is unsupported', () => {
    expect(create('unsupported').textContent?.trim()).toBe('');
  });

  it('offers to activate the reminders and asks for permission on click', () => {
    const el = create('off');

    expect(el.textContent).toContain('5 minutos antes de cada cita');
    clickButton(el, 'Activar recordatorios');

    expect(service.enable).toHaveBeenCalledTimes(1);
  });

  it('shows how to install the app on iPhone', () => {
    const el = create('needs-install');

    expect(el.textContent).toContain('Añadir a pantalla de inicio');
    expect(el.querySelector('button')).toBeNull();
  });

  it('explains how to unblock the permission', () => {
    const el = create('blocked');

    expect(el.textContent).toContain('Bloqueaste las notificaciones');
    expect(el.querySelector('button')).toBeNull();
  });

  it('shows the active state with test and deactivate actions', () => {
    const el = create('on');

    expect(el.textContent).toContain('Activados en este dispositivo');
    clickButton(el, 'Enviar prueba');
    expect(service.sendTest).toHaveBeenCalledTimes(1);
    expect(el.textContent).toContain('te enviamos una notificación de prueba');

    clickButton(el, 'Desactivar');
    expect(service.disable).toHaveBeenCalledTimes(1);
  });

  it('syncs the existing subscription when it is already on', () => {
    create('on');

    expect(service.syncExisting).toHaveBeenCalledTimes(1);
  });

  it('warns when the test could not be delivered', () => {
    service.sendTest.mockReturnValueOnce(of({ delivered: 0 }));
    const el = create('on');

    clickButton(el, 'Enviar prueba');

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos entregar la prueba');
  });

  it('shows an error when activation fails', () => {
    service.enable.mockReturnValueOnce(throwError(() => new Error('boom')));
    const el = create('off');

    clickButton(el, 'Activar recordatorios');

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No se pudieron activar');
  });

  it('shows no error message when the user simply denies the permission prompt', () => {
    service.enable.mockReturnValueOnce(throwError(() => new DOMException('denied', 'NotAllowedError')));
    const el = create('off');

    clickButton(el, 'Activar recordatorios');

    expect(el.querySelector('[role="alert"]')).toBeNull();
  });
});
