import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ComposerForm } from '../../models/composer-form.model';

export const MESSAGE_MAX_LENGTH = 500;
const COUNTER_VISIBLE_FROM = 400;

@Component({
  selector: 'app-chat-composer',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './chat-composer.component.html',
})
export class ChatComposerComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly disabled = input(false);
  readonly placeholder = input('Escribe un mensaje…');
  readonly sent = output<string>();

  protected readonly maxLength = MESSAGE_MAX_LENGTH;
  protected readonly form: FormGroup<ComposerForm> = this.fb.group({
    text: this.fb.control('', [Validators.required, Validators.maxLength(MESSAGE_MAX_LENGTH)]),
  });

  protected readonly length = toSignal(this.form.controls.text.valueChanges.pipe(map((value) => value.length)), {
    initialValue: 0,
  });
  protected readonly showCounter = computed(() => this.length() >= COUNTER_VISIBLE_FROM);

  /** Enter sends; Shift+Enter inserts a new line. */
  protected onEnter(event: Event): void {
    if (event instanceof KeyboardEvent && !event.shiftKey) {
      event.preventDefault();
      this.submit();
    }
  }

  protected submit(): void {
    const text = this.form.controls.text.value.trim();
    if (!text || this.disabled()) {
      return;
    }
    this.sent.emit(text);
    this.form.reset();
  }
}
