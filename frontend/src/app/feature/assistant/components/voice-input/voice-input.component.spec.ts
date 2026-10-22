import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EMPTY, Observable, of, throwError } from 'rxjs';
import { ErrorHandlerService } from '@/core/service/error-handler.service';
import { RecordingOutcome, TranscriptionResponseDto, VoiceRecording } from '../../models/voice.model';
import { AgentService } from '../../service/agent.service';
import { VoiceRecorderService } from '../../service/voice-recorder.service';
import { VoiceInputComponent } from './voice-input.component';

const RECORDING: VoiceRecording = { blob: new Blob(['audio']), mimeType: 'audio/webm;codecs=opus', durationMs: 4000 };

function httpError(status: number, message: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { message } });
}

describe('VoiceInputComponent', () => {
  let fixture: ComponentFixture<VoiceInputComponent>;
  let element: HTMLElement;
  let recorder: {
    recording: ReturnType<typeof signal<boolean>>;
    elapsedSeconds: ReturnType<typeof signal<number>>;
    level: ReturnType<typeof signal<number>>;
    record: ReturnType<typeof vi.fn<() => Promise<RecordingOutcome>>>;
    stop: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let transcribe: ReturnType<typeof vi.fn<(audio: Blob, mimeType: string) => Observable<TranscriptionResponseDto>>>;
  let transcribed: string[];

  beforeEach(() => {
    // isSupported() is static, so the browser APIs must exist for the button to render.
    vi.stubGlobal('MediaRecorder', class {});
    Object.defineProperty(navigator, 'mediaDevices', { value: { getUserMedia: vi.fn() }, configurable: true });

    recorder = {
      recording: signal(false),
      elapsedSeconds: signal(0),
      level: signal(0),
      record: vi.fn(async () => ({ status: 'recorded', recording: RECORDING }) as RecordingOutcome),
      stop: vi.fn(),
      cancel: vi.fn(),
    };
    transcribe = vi.fn(() => of({ text: 'Agenda a Juan mañana', remainingToday: 30 }));

    TestBed.configureTestingModule({
      imports: [VoiceInputComponent],
      providers: [
        { provide: AgentService, useValue: { transcribe, voiceQuota: () => EMPTY } },
        { provide: ErrorHandlerService, useValue: { extractMessage: (e: HttpErrorResponse) => e.error.message } },
      ],
    });
    TestBed.overrideComponent(VoiceInputComponent, {
      set: { providers: [{ provide: VoiceRecorderService, useValue: recorder }] },
    });
    fixture = TestBed.createComponent(VoiceInputComponent);
    element = fixture.nativeElement;
    transcribed = [];
    fixture.componentInstance.transcribed.subscribe((text) => transcribed.push(text));
    fixture.detectChanges();
  });

  afterEach(() => vi.unstubAllGlobals());

  function button(label: string): HTMLButtonElement | null {
    return element.querySelector(`button[aria-label="${label}"]`);
  }

  async function tapMicrophone(): Promise<void> {
    button('Dictar mensaje por voz')?.click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function alertText(): string {
    return element.querySelector('[role="alert"]')?.textContent ?? '';
  }

  it('uploads a recording with speech and emits the text for review', async () => {
    await tapMicrophone();

    expect(transcribe).toHaveBeenCalledWith(RECORDING.blob, RECORDING.mimeType);
    expect(transcribed).toEqual(['Agenda a Juan mañana']);
  });

  it('does not upload cancelled recordings', async () => {
    recorder.record.mockResolvedValueOnce({ status: 'cancelled' });

    await tapMicrophone();

    expect(transcribe).not.toHaveBeenCalled();
    expect(alertText()).toBe('');
  });

  it('does not upload recordings without speech and explains why', async () => {
    recorder.record.mockResolvedValueOnce({ status: 'no-speech' });

    await tapMicrophone();

    expect(transcribe).not.toHaveBeenCalled();
    expect(alertText()).toContain('No te escuchamos');
  });

  it('explains how to enable a denied microphone', async () => {
    recorder.record.mockRejectedValueOnce(new DOMException('denied', 'NotAllowedError'));

    await tapMicrophone();

    expect(alertText()).toContain('Permite el acceso al micrófono');
  });

  it('offers a manual retry that re-uploads the same recording when the provider fails', async () => {
    transcribe.mockReturnValueOnce(throwError(() => httpError(502, 'El dictado no está disponible')));

    await tapMicrophone();
    expect(alertText()).toContain('El dictado no está disponible');
    expect(transcribe).toHaveBeenCalledTimes(1);

    (Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes('Reintentar')) as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(transcribe).toHaveBeenCalledTimes(2);
    expect(transcribe.mock.calls[1][0]).toBe(RECORDING.blob);
    expect(transcribed).toEqual(['Agenda a Juan mañana']);
  });

  it('does not offer a retry when the quota is exhausted', async () => {
    transcribe.mockReturnValueOnce(throwError(() => httpError(429, 'Alcanzaste el límite de dictados por voz de hoy')));

    await tapMicrophone();

    expect(alertText()).toContain('Alcanzaste el límite');
    expect(alertText()).not.toContain('Reintentar');
  });

  it('shows the remaining dictations when few are left', async () => {
    transcribe.mockReturnValueOnce(of({ text: 'hola', remainingToday: 3 }));

    await tapMicrophone();

    expect(button('Dictar mensaje por voz')?.textContent).toContain('3');
  });

  it('shows the recording controls and cancels with Escape', () => {
    recorder.recording.set(true);
    recorder.elapsedSeconds.set(27);
    fixture.detectChanges();

    expect(button('Terminar dictado')).not.toBeNull();
    expect(element.textContent).toContain('0:27 / 0:30');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(recorder.cancel).toHaveBeenCalled();

    button('Terminar dictado')?.click();
    expect(recorder.stop).toHaveBeenCalled();
  });

  it('renders nothing when the browser cannot record', () => {
    vi.stubGlobal('MediaRecorder', undefined);
    const unsupported = TestBed.createComponent(VoiceInputComponent);
    unsupported.detectChanges();

    expect((unsupported.nativeElement as HTMLElement).querySelector('button')).toBeNull();
  });
});
