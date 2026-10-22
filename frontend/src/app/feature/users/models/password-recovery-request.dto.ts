/** Mirrors PasswordRecoveryRequestResponse. */
export interface PasswordRecoveryRequestDto {
  requestId: number;
  userId: number;
  username: string;
  name: string;
  lastName: string;
  phone: string;
  requestedAt: string;
}
