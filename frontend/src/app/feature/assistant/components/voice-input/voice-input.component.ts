import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { outputFromObservable, takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ErrorHandlerService } from '@/core/service/error-handler.service';
import { LOW_QUOTA_WARNING, VOICE_MAX_SECONDS, VOICE_WARNING_SECONDS } from '../../models/voice.const';
import { RecordingOutcome, VoiceError, VoiceRecording } from '../../models/voice.model';
import { AgentService } from '../../service/agent.service';
import { VoiceRecorderService } from '../../service/voice-recorder.service';

/** Relative height of each level-meter bar; the middle ones react the most. */
const BAR_WEIGHTS = [0.45, 0.75, 1, 0.75, 0.45];
const MIN_BAR_HEIGHT = 0.15;
const SECONDS_PER_MINUTE = 60;
const SERVER_ERROR_FROM = 500;
const NETWORK_ERROR_STATUS = 0;

const MESSAGES = {
  tooShort: 'Muy corto: habla un poco más antes de terminar',
  noSpeech: 'No te escuchamos. Acércate al micrófono e inténtalo de nuevo',
  permissionDenied: 'Permite el acceso al micrófono en tu navegador para dictar',
  noMicrophone: 'No se encontró un micrófono',
  microphoneFailed: 'No se pudo usar el micrófono',
} as const;

/**
 * Dictation button for the chat composer: records (max 30 s), uploads the audio and emits the text so
 * the user can review it before sending. Errors are shown inline; only recordings with speech are
 * uploaded, and a failed upload is retried only when the user asks for it.
 */
@Component({
  selector: 'app-voice-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './voice-input.component.html',
  providers: [VoiceRecorderService],
  host: {
    class: 'relative flex',
    '[class.flex-1]': 'recording()',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class VoiceInputComponent {
  private readonly recorder = inject(VoiceRecorderService);
  private readonly agent = inject(AgentService);
  private readonly errorHandler = inject(ErrorHandlerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly disabled = input(false);
  readonly transcribed = output<string>();

  protected readonly supported = VoiceRecorderService.isSupported();
  protected readonly maxSeconds = VOICE_MAX_SECONDS;
  protected readonly recording = this.recorder.recording;
  protected readonly requesting = signal(false);
  protected readonly transcribing = signal(false);
  protected readonly error = signal<VoiceError | null>(null);
  protected readonly remainingToday = signal<number | null>(null);

  /** The composer hides the text field while recording. */
  readonly recordingChange = outputFromObservable(toObservable(this.recording));

  protected readonly elapsedLabel = computed(() => formatSeconds(this.recorder.elapsedSeconds()));
  protected readonly maxLabel = formatSeconds(VOICE_MAX_SECONDS);
  protected readonly nearLimit = computed(() => VOICE_MAX_SECONDS - this.recorder.elapsedSeconds() <= VOICE_WARNING_SECONDS);
  protected readonly bars = computed(() => BAR_WEIGHTS.map((weight) => Math.max(MIN_BAR_HEIGHT, this.recorder.level() * weight)));
  protected readonly lowQuota = computed(() => {
    const remaining = this.remainingToday();
    return remaining !== null && remaining <= LOW_QUOTA_WARNING;
  });
  protected readonly micDisabled = computed(() => this.disabled() || this.requesting() || this.transcribing());

  private lastRecording: VoiceRecording | null = null;

  protected async start(): Promise<void> {
    if (this.micDisabled() || this.recording()) {
      return;
    }
    this.error.set(null);
    this.requesting.set(true);
    let outcome: RecordingOutcome;
    try {
      outcome = await this.recorder.record();
    } catch (error) {
      this.error.set({ message: microphoneErrorMessage(error), retryable: false });
      return;
    } finally {
      this.requesting.set(false);
    }
    this.handleOutcome(outcome);
  }

  protected finish(): void {
    this.recorder.stop();
  }

  protected cancel(): void {
    this.recorder.cancel();
  }

  /** Re-uploads the same recording; never automatic, so a failure is never paid twice behind the user's back. */
  protected retry(): void {
    if (this.lastRecording && !this.transcribing()) {
      this.upload(this.lastRecording);
    }
  }

  protected dismissError(): void {
    this.error.set(null);
    this.lastRecording = null;
  }

  protected onEscape(): void {
    if (this.recording()) {
      this.cancel();
    }
  }

  private handleOutcome(outcome: RecordingOutcome): void {
    switch (outcome.status) {
      case 'recorded':
        this.upload(outcome.recording);
        break;
      case 'too-short':
        this.error.set({ message: MESSAGES.tooShort, retryable: false });
        break;
      case 'no-speech':
        this.error.set({ message: MESSAGES.noSpeech, retryable: false });
        break;
      case 'cancelled':
        break;
    }
  }

  private upload(recording: VoiceRecording): void {
    this.lastRecording = recording;
    this.error.set(null);
    this.transcribing.set(true);
    this.agent
      .transcribe(recording.blob, recording.mimeType)
      .pipe(
        finalize(() => this.transcribing.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.lastRecording = null;
          this.remainingToday.set(response.remainingToday);
          this.transcribed.emit(response.text);
        },
        error: (error: HttpErrorResponse) => {
          const retryable = isRetryable(error);
          if (!retryable) {
            this.lastRecording = null;
          }
          this.error.set({ message: this.errorHandler.extractMessage(error), retryable });
        },
      });
  }
}

/** Network failures and 5xx can succeed on a second try; 4xx (invalid audio, quota) cannot. */
function isRetryable(error: HttpErrorResponse): boolean {
  return error.status === NETWORK_ERROR_STATUS || error.status >= SERVER_ERROR_FROM;
}

function microphoneErrorMessage(error: unknown): string {
  const name = error instanceof DOMException ? error.name : '';
  switch (name) {
    case 'NotAllowedError':
    case 'SecurityError':
      return MESSAGES.permissionDenied;
    case 'NotFoundError':
    case 'OverconstrainedError':
      return MESSAGES.noMicrophone;
    default:
      return MESSAGES.microphoneFailed;
  }
}

function formatSeconds(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / SECONDS_PER_MINUTE);
  const seconds = totalSeconds % SECONDS_PER_MINUTE;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}
