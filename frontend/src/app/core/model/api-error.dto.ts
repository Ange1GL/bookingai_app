/** Mirrors the backend ErrorResponse record. */
export interface ApiErrorDto {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
