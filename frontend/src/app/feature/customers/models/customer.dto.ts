/** Mirrors CustomerListItemResponse. */
export interface CustomerListItemDto {
  id: number;
  name: string;
  phone: string;
  blacklisted: boolean;
}

/** Mirrors CustomerPageResponse. */
export interface CustomerPageDto {
  content: CustomerListItemDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Mirrors CustomerResponse. */
export interface CustomerDto {
  id: number;
  name: string;
  phone: string;
}

/** Mirrors CreateCustomerRequest. */
export interface CreateCustomerRequestDto {
  name: string;
  phone: string;
}

export const NAME_MAX_LENGTH = 100;
export const PHONE_MAX_LENGTH = 20;
export const BLACKLIST_REASON_MAX_LENGTH = 250;
export const PHONE_PATTERN = /^\+?[\d\s()-]+$/;
export const PAGE_SIZE = 10;
