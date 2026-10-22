/** Mirrors VerifyCodeRequest: step 1 of creating the password. */
export interface VerifyCodeRequestDto {
  username: string;
  code: string;
}

/** Mirrors VerifyCodeResponse. `expiresAt` is an ISO-8601 instant; after it the ticket is useless. */
export interface VerifyCodeResponseDto {
  verificationToken: string;
  expiresAt: string;
}

export const USERNAME_MAX_LENGTH = 50;
