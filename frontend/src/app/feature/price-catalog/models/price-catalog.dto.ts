/** Mirrors PriceCatalogResponse. */
export interface PriceCatalogItemDto {
  id: number;
  label: string;
  price: number;
}

/** Mirrors PriceCatalogRequest (POST and PUT). */
export interface PriceCatalogRequestDto {
  label: string;
  price: number;
}

export const LABEL_MAX_LENGTH = 100;
export const PRICE_MIN = 1;
export const PRICE_MAX = 9_999_999;
