/** One labelled value shown by `app-detail-dialog`. A null/empty value renders as an em dash. */
export interface DetailField {
  label: string;
  value: string | null | undefined;
  /** PrimeIcons class, e.g. `pi-phone`. */
  icon?: string;
}
