export const PAGE_GAP = 'gap';
export type PageItem = number | typeof PAGE_GAP;

const SIBLINGS = 1;
/** first + last + current + 2 siblings + 2 gaps: below this every page fits without ellipsis. */
const MAX_WITHOUT_GAPS = 7;

/** Zero-based page numbers to render, collapsing long ranges into gaps: 0 … 4 5 6 … 20. */
export function buildPageItems(current: number, totalPages: number): PageItem[] {
  if (totalPages <= MAX_WITHOUT_GAPS) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }
  const last = totalPages - 1;
  const start = Math.max(1, Math.min(current - SIBLINGS, last - SIBLINGS * 2 - 2));
  const end = Math.min(last - 1, Math.max(current + SIBLINGS, 2 + SIBLINGS * 2));

  const items: PageItem[] = [0];
  if (start > 1) {
    items.push(PAGE_GAP);
  }
  for (let page = start; page <= end; page++) {
    items.push(page);
  }
  if (end < last - 1) {
    items.push(PAGE_GAP);
  }
  items.push(last);
  return items;
}
