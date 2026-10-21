/** Mirrors the backend AuthSuccessResponse record. */
export interface AuthUserDto {
  userId: number;
  username: string;
  email: string;
  roles: string[];
  /** Permission names granted through the roles; a UI hint only, the backend enforces them on every call. */
  permissions: string[];
}
