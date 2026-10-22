import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { REMINDER_LEAD_MINUTES } from '@/core/model/push.model';
import { PushNotificationsService } from '@/core/service/push-notifications.service';

const MESSAGES = {
  unavailable: 'Las notificaciones aún no están disponibles. Inténtalo más tarde.',
  failed: 'No se pudieron activar las notificaciones. Inténtalo de nuevo.',
  testSent: 'Listo, te enviamos una notificación de prueba.',
  testNotDelivered: 'No pudimos entregar la prueba. Desactiva y vuelve a activar los recordatorios.',
  testFailed: 'No se pudo enviar la prueba. Inténtalo de nuevo.',
} as const;

interface Feedback {
  text: string;
  tone: 'success' | 'error';
}

/**
 * Asks for the browser's notification permission and subscribes this device to appointment reminders.
 * Renders nothing where Web Push cannot work (unsupported browsers, `ng serve`).
 */
@Component({
  selector: 'app-push-notifications-card',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './push-notifications-card.component.html',
})
export class PushNotificationsCardComponent {
  private readonly push = inject(PushNotificationsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly status = this.push.status;
  protected readonly leadMinutes = REMINDER_LEAD_MINUTES;
  protected readonly busy = signal(false);
  protected readonly feedback = signal<Feedback | null>(null);

  constructor() {
    effect(() => {
      if (this.status() === 'on') {
        this.push.syncExisting().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ error: () => undefined });
      }
    });
  }

  protected enable(): void {
    this.run(this.push.enable(), () => undefined, (error) => enableErrorMessage(error));
  }

  protected disable(): void {
    this.run(this.push.disable(), () => undefined, () => MESSAGES.failed);
  }

  protected sendTest(): void {
    this.run(
      this.push.sendTest(),
      (response) => ({
        text: response.delivered > 0 ? MESSAGES.testSent : MESSAGES.testNotDelivered,
        tone: response.delivered > 0 ? 'success' : 'error',
      }),
      () => MESSAGES.testFailed,
    );
  }

  private run<T>(
    request: Observable<T>,
    onSuccess: (value: T) => Feedback | void,
    errorMessage: (error: unknown) => string,
  ): void {
    this.busy.set(true);
    this.feedback.set(null);
    request
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (value) => this.feedback.set(onSuccess(value) ?? null),
        error: (error: unknown) => this.feedback.set({ text: errorMessage(error), tone: 'error' }),
      });
  }
}

function enableErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.ServiceUnavailable) {
    return MESSAGES.unavailable;
  }
  // A denied prompt needs no message here: the status becomes `blocked` and the card explains how to undo it.
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return '';
  }
  return MESSAGES.failed;
}
