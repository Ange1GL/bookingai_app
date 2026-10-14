import { TestBed } from '@angular/core/testing';
import {
  AUDIO_BITS_PER_SECOND,
  MIN_RECORDING_MS,
  NO_SPEECH_TIMEOUT_MS,
  SILENCE_AUTO_STOP_MS,
  VOICE_MAX_SECONDS,
} from '../models/voice.const';
import { RecordingOutcome } from '../models/voice.model';
import { VoiceRecorderService } from './voice-recorder.service';

const SPEECH = 0.2;
const SILENCE = 0;
let amplitude = SILENCE;

class FakeMediaRecorder {
  static supportedTypes = new Set(['audio/webm;codecs=opus']);
  static last: FakeMediaRecorder;
  static isTypeSupported = (type: string) => FakeMediaRecorder.supportedTypes.has(type);

  state: RecordingState = 'inactive';
  ondataavailable: ((event: BlobEvent) => void) | null = null;
  onstop: (() => void) | null = null;
  readonly mimeType: string;

  constructor(
    readonly stream: MediaStream,
    readonly options: MediaRecorderOptions,
  ) {
    this.mimeType = options.mimeType ?? '';
    FakeMediaRecorder.last = this;
  }

  start(): void {
    this.state = 'recording';
  }

  stop(): void {
    this.state = 'inactive';
    this.ondataavailable?.({ data: new Blob(['audio-bytes']) } as BlobEvent);
    this.onstop?.();
  }
}

class FakeAnalyser {
  readonly fftSize = 32;
  getFloatTimeDomainData(samples: Float32Array): void {
    samples.fill(amplitude);
  }
}

class FakeAudioContext {
  static last: FakeAudioContext;
  state: AudioContextState = 'running';
  readonly close = vi.fn(async () => {
    this.state = 'closed';
  });

  constructor() {
    FakeAudioContext.last = this;
  }

  createAnalyser(): FakeAnalyser {
    return new FakeAnalyser();
  }

  createMediaStreamSource(): { connect: () => void } {
    return { connect: vi.fn() };
  }
}

describe('VoiceRecorderService', () => {
  let service: VoiceRecorderService;
  let track: { stop: ReturnType<typeof vi.fn> };
  let getUserMedia: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers();
    amplitude = SILENCE;
    track = { stop: vi.fn() };
    getUserMedia = vi.fn(async () => ({ getTracks: () => [track] }) as unknown as MediaStream);
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder);
    vi.stubGlobal('AudioContext', FakeAudioContext);
    Object.defineProperty(navigator, 'mediaDevices', { value: { getUserMedia }, configurable: true });
    TestBed.configureTestingModule({ providers: [VoiceRecorderService] });
    service = TestBed.inject(VoiceRecorderService);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  /** Starts recording and lets getUserMedia resolve. Wrapped: an async function would await the outcome itself. */
  async function startRecording(): Promise<{ outcome: Promise<RecordingOutcome> }> {
    const outcome = service.record();
    await vi.advanceTimersByTimeAsync(0);
    return { outcome };
  }

  it('reports support only when MediaRecorder and getUserMedia exist', () => {
    expect(VoiceRecorderService.isSupported()).toBe(true);
    vi.stubGlobal('MediaRecorder', undefined);
    expect(VoiceRecorderService.isSupported()).toBe(false);
  });

  it('records with the first supported MIME type and a low bitrate', async () => {
    const { outcome } = await startRecording();

    expect(service.recording()).toBe(true);
    expect(FakeMediaRecorder.last.options).toEqual({ mimeType: 'audio/webm;codecs=opus', audioBitsPerSecond: AUDIO_BITS_PER_SECOND });
    service.cancel();
    await outcome;
  });

  it('stops by itself after a pause following speech and releases the microphone', async () => {
    const { outcome } = await startRecording();
    amplitude = SPEECH;
    await vi.advanceTimersByTimeAsync(1500);
    amplitude = SILENCE;
    await vi.advanceTimersByTimeAsync(SILENCE_AUTO_STOP_MS + 100);

    const result = await outcome;

    expect(result.status).toBe('recorded');
    if (result.status === 'recorded') {
      expect(result.recording.mimeType).toBe('audio/webm;codecs=opus');
      expect(result.recording.blob.type).toBe('audio/webm;codecs=opus');
    }
    expect(track.stop).toHaveBeenCalled();
    expect(FakeAudioContext.last.close).toHaveBeenCalled();
    expect(service.recording()).toBe(false);
  });

  it('discards the recording when no speech is heard', async () => {
    const { outcome } = await startRecording();
    await vi.advanceTimersByTimeAsync(NO_SPEECH_TIMEOUT_MS + 100);

    expect(await outcome).toEqual({ status: 'no-speech' });
    expect(track.stop).toHaveBeenCalled();
  });

  it('stops at the time cap even if the user keeps talking', async () => {
    const { outcome } = await startRecording();
    amplitude = SPEECH;
    await vi.advanceTimersByTimeAsync(VOICE_MAX_SECONDS * 1000 + 100);

    const result = await outcome;

    expect(result.status).toBe('recorded');
    if (result.status === 'recorded') {
      expect(result.recording.durationMs).toBeGreaterThanOrEqual(VOICE_MAX_SECONDS * 1000);
    }
  });

  it('marks very short recordings as too short', async () => {
    const { outcome } = await startRecording();
    amplitude = SPEECH;
    await vi.advanceTimersByTimeAsync(MIN_RECORDING_MS / 2);
    service.stop();

    expect(await outcome).toEqual({ status: 'too-short' });
  });

  it('cancels without keeping the audio', async () => {
    const { outcome } = await startRecording();
    amplitude = SPEECH;
    await vi.advanceTimersByTimeAsync(3000);
    service.cancel();

    expect(await outcome).toEqual({ status: 'cancelled' });
    expect(track.stop).toHaveBeenCalled();
  });

  it('exposes the elapsed seconds and the input level while recording', async () => {
    const { outcome } = await startRecording();
    amplitude = SPEECH;
    await vi.advanceTimersByTimeAsync(2050);

    expect(service.elapsedSeconds()).toBe(2);
    expect(service.level()).toBeGreaterThan(0);
    service.cancel();
    await outcome;
    expect(service.elapsedSeconds()).toBe(0);
    expect(service.level()).toBe(0);
  });

  it('rejects when the microphone is denied', async () => {
    getUserMedia.mockRejectedValueOnce(new DOMException('denied', 'NotAllowedError'));

    await expect(service.record()).rejects.toMatchObject({ name: 'NotAllowedError' });
    expect(service.recording()).toBe(false);
  });
});
