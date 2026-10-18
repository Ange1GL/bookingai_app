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

export const BLACKLIST_REASON_MAX_LENGTH = 250;
export const PAGE_SIZE = 10;
