/** Mirrors the backend CreatePasswordRequest: the one-time code received by WhatsApp plus the chosen password. */
export interface CreatePasswordRequestDto {
  username: string;
  code: string;
  newPassword: string;
}

/** The backend generates 6-digit codes (SecureRandomOneTimeCodeGenerator). */
export const OTP_LENGTH = 6;
export const OTP_PATTERN = /^\d{6}$/;
