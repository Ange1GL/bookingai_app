/** Mirrors the backend AuthSuccessResponse record. */
export interface AuthUserDto {
  userId: number;
  username: string;
  email: string;
  roles: string[];
}
