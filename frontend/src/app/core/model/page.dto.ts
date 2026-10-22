/** Mirrors the backend PageResponse record used by the administrative listings. */
export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
