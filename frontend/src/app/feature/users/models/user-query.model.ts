export type ActiveFilter = 'all' | 'active' | 'inactive';

export interface UserQuery {
  search: string;
  status: ActiveFilter;
  page: number;
}

export const INITIAL_USER_QUERY: UserQuery = { search: '', status: 'all', page: 0 };

/** Maps the UI filter to the backend `active` param (`null` = do not filter). */
export function toActiveParam(status: ActiveFilter): boolean | null {
  if (status === 'all') {
    return null;
  }
  return status === 'active';
}
