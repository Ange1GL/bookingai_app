/** Hard cap per recording; mirrors the backend size limit (30 s of opus ≈ 110 KB). */
export const VOICE_MAX_SECONDS = 30;

/** The countdown turns red during the last seconds. */
export const VOICE_WARNING_SECONDS = 5;

/** Recording stops by itself after this much silence following speech (saves paid seconds). */
export const SILENCE_AUTO_STOP_MS = 2000;

/** If no speech is heard this long after starting, the recording is discarded without uploading. */
export const NO_SPEECH_TIMEOUT_MS = 6000;

/** Shorter recordings are accidental taps: discarded without uploading. */
export const MIN_RECORDING_MS = 800;

/** Opus at 24 kbps is plenty for speech; the provider bills by duration, not size. */
export const AUDIO_BITS_PER_SECOND = 24_000;

/** How often the recorder samples the volume (silence detection and the level meter). */
export const MONITOR_INTERVAL_MS = 100;

/** RMS volume (0..1) above which the input counts as speech. */
export const SPEECH_RMS_THRESHOLD = 0.015;

/** Scales the RMS volume into the 0..1 range used by the level meter. */
export const LEVEL_GAIN = 8;

/** Remaining daily dictations at or below this number are shown to the user. */
export const LOW_QUOTA_WARNING = 5;

/** In order of preference; Safari (iOS) only records `audio/mp4`. */
export const PREFERRED_MIME_TYPES = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4'];

/** File extension sent to the backend per base MIME type. */
export const AUDIO_EXTENSIONS: Record<string, string> = {
  'audio/webm': 'webm',
  'audio/ogg': 'ogg',
  'audio/mp4': 'mp4',
};
