export interface RegisterRequestDto {
  username: string;
  email: string;
  password: string;
  name: string;
}

export const PASSWORD_MIN_LENGTH = 8;
