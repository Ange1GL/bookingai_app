import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ClipboardService } from '@/core/service/clipboard.service';

const COPIED_FEEDBACK_MS = 2000;

/** Button that copies `value` to the clipboard and confirms it with a brief "Copiado" state. */
@Component({
  selector: 'app-copy-button',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './copy-button.component.html',
})
export class CopyButtonComponent {
  private readonly clipboard = inject(ClipboardService);
  private readonly messages = inject(MessageService);
  private resetTimer: ReturnType<typeof setTimeout> | undefined;

  readonly value = input.required<string>();
  readonly label = input('Copiar');
  readonly ariaLabel = input<string | null>(null);
  readonly size = input<'small' | 'large' | undefined>('small');
  readonly outlined = input(true);

  protected readonly copied = signal(false);
  protected readonly icon = computed(() => (this.copied() ? 'pi pi-check' : 'pi pi-copy'));
  protected readonly text = computed(() => (this.copied() ? 'Copiado' : this.label()));

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.resetTimer));
  }

  protected async copy(): Promise<void> {
    if (!(await this.clipboard.copy(this.value()))) {
      this.messages.add({ severity: 'warn', summary: 'Aviso', detail: 'No se pudo copiar. Selecciónalo y cópialo manualmente.' });
      return;
    }
    this.copied.set(true);
    clearTimeout(this.resetTimer);
    this.resetTimer = setTimeout(() => this.copied.set(false), COPIED_FEEDBACK_MS);
  }
}
