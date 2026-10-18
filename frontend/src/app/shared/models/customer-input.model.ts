/** Mirrors CreateCustomerRequest. */
export interface CreateCustomerRequestDto {
  name: string;
  phone: string;
}

export const NAME_MAX_LENGTH = 100;
export const PHONE_MAX_LENGTH = 20;
export const PHONE_PATTERN = /^\+?[\d\s()-]+$/;
