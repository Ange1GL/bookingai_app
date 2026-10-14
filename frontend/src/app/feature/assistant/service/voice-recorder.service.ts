import { Injectable, OnDestroy, signal } from '@angular/core';
import {
  AUDIO_BITS_PER_SECOND,
  LEVEL_GAIN,
  MIN_RECORDING_MS,
  MONITOR_INTERVAL_MS,
  NO_SPEECH_TIMEOUT_MS,
  PREFERRED_MIME_TYPES,
  SILENCE_AUTO_STOP_MS,
  SPEECH_RMS_THRESHOLD,
  VOICE_MAX_SECONDS,
} from '../models/voice.const';
import { RecordingOutcome } from '../models/voice.model';

type EndReason = 'stop' | 'cancel';
type AudioContextConstructor = typeof AudioContext;

const AUDIO_CONSTRAINTS: MediaStreamConstraints = {
  audio: { echoCancellation: true, noiseSuppression: true, channelCount: 1 },
};
const MS_PER_SECOND = 1000;

/**
 * Records one dictation with MediaRecorder. Besides the hard cap it watches the volume to stop by
 * itself after a pause and to discard recordings without speech, so silence is never uploaded (or paid).
 * Provided by the voice-input component, so each instance owns its microphone lifecycle.
 */
@Injectable()
export class VoiceRecorderService implements OnDestroy {
  private readonly recordingState = signal(false);
  private readonly elapsedState = signal(0);
  private readonly levelState = signal(0);

  readonly recording = this.recordingState.asReadonly();
  readonly elapsedSeconds = this.elapsedState.asReadonly();
  /** Input volume in 0..1 for the level meter. */
  readonly level = this.levelState.asReadonly();

  private stream: MediaStream | null = null;
  private recorder: MediaRecorder | null = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private samples: Float32Array<ArrayBuffer> | null = null;
  private monitorId: ReturnType<typeof setInterval> | null = null;
  private chunks: Blob[] = [];
  private startedAt = 0;
  private lastSpeechAt = 0;
  private heardSpeech = false;
  private endReason: EndReason = 'stop';
  private resolveOutcome: ((outcome: RecordingOutcome) => void) | null = null;

  static isSupported(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia && typeof MediaRecorder !== 'undefined';
  }

  /**
   * Asks for the microphone and records until {@link stop}, {@link cancel}, the time cap or a pause.
   * Rejects with the browser's `DOMException` when the microphone can't be opened.
   */
  async record(): Promise<RecordingOutcome> {
    if (this.recorder) {
      throw new Error('A recording is already in progress');
    }
    this.stream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS);
    try {
      return await new Promise<RecordingOutcome>((resolve) => {
        this.resolveOutcome = resolve;
        this.startRecorder(this.stream as MediaStream);
      });
    } finally {
      this.release();
    }
  }

  /** Ends the recording and keeps it (it resolves `record()` as recorded, too short or without speech). */
  stop(): void {
    if (this.recorder?.state === 'recording') {
      this.clearMonitor();
      this.recorder.stop();
    }
  }

  /** Ends the recording and throws it away: nothing is uploaded. */
  cancel(): void {
    this.endReason = 'cancel';
    this.stop();
  }

  ngOnDestroy(): void {
    this.cancel();
    this.release();
  }

  private startRecorder(stream: MediaStream): void {
    const preferredType = PREFERRED_MIME_TYPES.find((type) => MediaRecorder.isTypeSupported(type));
    const recorder = new MediaRecorder(stream, {
      ...(preferredType ? { mimeType: preferredType } : {}),
      audioBitsPerSecond: AUDIO_BITS_PER_SECOND,
    });
    recorder.ondataavailable = (event: BlobEvent) => {
      if (event.data.size > 0) {
        this.chunks.push(event.data);
      }
    };
    recorder.onstop = () => this.finish(recorder.mimeType || preferredType || '');

    this.recorder = recorder;
    this.chunks = [];
    this.endReason = 'stop';
    this.heardSpeech = false;
    this.startAnalyser(stream);
    this.startedAt = Date.now();
    this.lastSpeechAt = this.startedAt;
    recorder.start();
    this.recordingState.set(true);
    this.monitorId = setInterval(() => this.monitor(), MONITOR_INTERVAL_MS);
  }

  /** Volume analysis is optional: without Web Audio only the time cap applies. */
  private startAnalyser(stream: MediaStream): void {
    const AudioContextImpl = audioContextConstructor();
    if (!AudioContextImpl) {
      return;
    }
    this.audioContext = new AudioContextImpl();
    this.analyser = this.audioContext.createAnalyser();
    this.audioContext.createMediaStreamSource(stream).connect(this.analyser);
    this.samples = new Float32Array(this.analyser.fftSize);
  }

  private monitor(): void {
    const now = Date.now();
    const elapsedMs = now - this.startedAt;
    this.elapsedState.set(Math.min(VOICE_MAX_SECONDS, Math.floor(elapsedMs / MS_PER_SECOND)));
    if (elapsedMs >= VOICE_MAX_SECONDS * MS_PER_SECOND) {
      this.stop();
      return;
    }

    const rms = this.readRms();
    if (rms === null) {
      return;
    }
    this.levelState.set(Math.min(1, rms * LEVEL_GAIN));
    if (rms >= SPEECH_RMS_THRESHOLD) {
      this.heardSpeech = true;
      this.lastSpeechAt = now;
    } else if (this.heardSpeech ? now - this.lastSpeechAt >= SILENCE_AUTO_STOP_MS : elapsedMs >= NO_SPEECH_TIMEOUT_MS) {
      this.stop();
    }
  }

  private readRms(): number | null {
    if (!this.analyser || !this.samples) {
      return null;
    }
    this.analyser.getFloatTimeDomainData(this.samples);
    let sumOfSquares = 0;
    for (const sample of this.samples) {
      sumOfSquares += sample * sample;
    }
    return Math.sqrt(sumOfSquares / this.samples.length);
  }

  private finish(mimeType: string): void {
    const durationMs = Date.now() - this.startedAt;
    this.resolveOutcome?.(this.outcome(mimeType, durationMs));
    this.resolveOutcome = null;
  }

  private outcome(mimeType: string, durationMs: number): RecordingOutcome {
    if (this.endReason === 'cancel') {
      return { status: 'cancelled' };
    }
    if (durationMs < MIN_RECORDING_MS) {
      return { status: 'too-short' };
    }
    if (this.analyser && !this.heardSpeech) {
      return { status: 'no-speech' };
    }
    return { status: 'recorded', recording: { blob: new Blob(this.chunks, { type: mimeType }), mimeType, durationMs } };
  }

  /** Always turns the microphone off, so the browser's recording indicator disappears. */
  private release(): void {
    this.clearMonitor();
    this.stream?.getTracks().forEach((track) => track.stop());
    if (this.audioContext && this.audioContext.state !== 'closed') {
      // close() only rejects when the context is already closed, which the guard above rules out.
      void this.audioContext.close();
    }
    this.stream = null;
    this.recorder = null;
    this.audioContext = null;
    this.analyser = null;
    this.samples = null;
    this.chunks = [];
    this.recordingState.set(false);
    this.elapsedState.set(0);
    this.levelState.set(0);
  }

  private clearMonitor(): void {
    if (this.monitorId !== null) {
      clearInterval(this.monitorId);
      this.monitorId = null;
    }
  }
}

function audioContextConstructor(): AudioContextConstructor | undefined {
  if (typeof window === 'undefined') {
    return undefined;
  }
  const legacyWindow = window as Window & { webkitAudioContext?: AudioContextConstructor };
  return window.AudioContext ?? legacyWindow.webkitAudioContext;
}
