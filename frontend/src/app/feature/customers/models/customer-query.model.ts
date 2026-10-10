export type BlacklistFilter = 'all' | 'blacklisted';

export interface CustomerFilters {
  /** Free text typed by the user; routed to `name` or `phone` by `toSearchParams`. */
  search: string;
  blacklist: BlacklistFilter;
}

export interface CustomerQuery extends CustomerFilters {
  page: number;
}

export const INITIAL_FILTERS: CustomerFilters = { search: '', blacklist: 'all' };

const PHONE_LIKE = /^[+\d\s()-]+$/;

/** The API filters name and phone separately; a digits-only input is treated as a phone. */
export function toSearchParams(search: string): { name?: string; phone?: string } {
  const term = search.trim();
  if (!term) {
    return {};
  }
  return PHONE_LIKE.test(term) ? { phone: term } : { name: term };
}
