import { PageDto } from '@/core/model/page.dto';

/** Mirrors RoleSummaryResponse. */
export interface RoleSummaryDto {
  id: number;
  name: string;
}

/** Mirrors UserResponse. */
export interface UserDto {
  id: number;
  username: string;
  name: string;
  lastName: string;
  email: string | null;
  phone: string;
  active: boolean;
  passwordSet: boolean;
  roles: RoleSummaryDto[];
}

export type UserPageDto = PageDto<UserDto>;

/**
 * Mirrors UserOneTimeCodeResponse. `oneTimeCode` and `shareMessage` only come back when the code was not sent by
 * WhatsApp (manual delivery), and they are shown once: the backend keeps just the hash.
 */
export interface UserOneTimeCodeDto {
  user: UserDto;
  codeSent: boolean;
  oneTimeCode?: string;
  shareMessage?: string;
}

/** Mirrors CreateUserRequest. */
export interface CreateUserRequestDto {
  username: string;
  name: string;
  lastName: string;
  email: string | null;
  phone: string;
  roleIds: number[];
}

export const USER_PAGE_SIZE = 10;
export const USERNAME_MIN_LENGTH = 3;
export const USERNAME_MAX_LENGTH = 50;
/** Usernames cannot contain whitespace. */
export const USERNAME_PATTERN = /^\S+$/;
export const PERSON_NAME_MAX_LENGTH = 255;
export const EMAIL_MAX_LENGTH = 255;
export const USER_PHONE_LENGTH = 10;
/** National number: exactly 10 digits, no country code. */
export const USER_PHONE_PATTERN = /^\d{10}$/;
export const USER_SEARCH_MAX_LENGTH = 100;

export function fullName(user: Pick<UserDto, 'name' | 'lastName'>): string {
  return `${user.name} ${user.lastName}`.trim();
}
