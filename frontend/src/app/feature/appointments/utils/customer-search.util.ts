const PHONE_LIKE = /^[+\d\s()-]+$/;

/** The customers API filters name and phone separately; a digits-only term is treated as a phone. */
export function toCustomerSearchParams(term: string): { name?: string; phone?: string } {
  const trimmed = term.trim();
  if (!trimmed) {
    return {};
  }
  return PHONE_LIKE.test(trimmed) ? { phone: trimmed } : { name: trimmed };
}
