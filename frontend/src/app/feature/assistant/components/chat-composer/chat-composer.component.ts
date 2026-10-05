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
  template: `
    <form
      [formGroup]="form"
      (ngSubmit)="submit()"
      class="flex items-end gap-2 rounded-3xl border border-surface bg-surface-0 p-1.5 shadow-sm transition focus-within:border-primary focus-within:shadow-md focus-within:shadow-primary-500/10 dark:bg-surface-900"
      novalidate
    >
      <label for="assistant-message" class="sr-only">Escribe tu mensaje</label>
      <textarea
        id="assistant-message"
        formControlName="text"
        rows="1"
        autocomplete="off"
        enterkeyhint="send"
        [attr.maxlength]="maxLength"
        [placeholder]="placeholder()"
        class="max-h-32 min-h-11 min-w-0 flex-1 resize-none self-center bg-transparent px-3 py-2.5 text-base leading-snug outline-none field-sizing-content placeholder:text-muted-color"
        (keydown.enter)="onEnter($event)"
      ></textarea>
      @if (showCounter()) {
        <span class="self-center pr-1 text-xs tabular-nums text-muted-color">{{ length() }}/{{ maxLength }}</span>
      }
      <button
        type="submit"
        class="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full bg-linear-to-br from-primary-500 to-primary-700 text-white shadow-md shadow-primary-500/30 transition active:scale-95 disabled:cursor-not-allowed disabled:from-surface-200 disabled:to-surface-300 disabled:shadow-none disabled:text-muted-color"
        aria-label="Enviar mensaje"
        [disabled]="disabled() || form.invalid"
      >
        <i class="pi pi-send" aria-hidden="true"></i>
      </button>
    </form>
  `,
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
