export interface LoginRequestDto {
  username: string;
  password: string;
}

/** Usernames are free-form on the backend; the login only rejects blanks and whitespace. */
export const USERNAME_PATTERN = /^\S+$/;
