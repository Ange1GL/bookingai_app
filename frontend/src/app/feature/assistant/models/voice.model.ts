/** Mirrors the backend TranscriptionResponse record. */
export interface TranscriptionResponseDto {
  text: string;
  remainingToday: number;
}

export interface VoiceRecording {
  blob: Blob;
  mimeType: string;
  durationMs: number;
}

/**
 * How a recording ended. Only `recorded` is uploaded (and billed); the rest cost nothing.
 * `no-speech` = the volume never reached speech level.
 */
export type RecordingOutcome =
  | { status: 'recorded'; recording: VoiceRecording }
  | { status: 'cancelled' }
  | { status: 'too-short' }
  | { status: 'no-speech' };

export type VoiceInputState = 'idle' | 'recording' | 'transcribing';

/** Error shown under the composer; `retryable` errors offer re-uploading the same recording. */
export interface VoiceError {
  message: string;
  retryable: boolean;
}
