/** Subset of the customers list item (CustomerListItemResponse) needed to pick who the appointment is for. */
export interface CustomerOptionDto {
  id: number;
  name: string;
  phone: string;
  blacklisted: boolean;
}

/** Subset of the customers page (CustomerPageResponse). */
export interface CustomerOptionsPageDto {
  content: CustomerOptionDto[];
}

/** Mirrors PriceCatalogResponse; the service an appointment is booked for. */
export interface ServiceOptionDto {
  id: number;
  label: string;
  price: number;
}

/** Backend page-size cap, so the dropdown shows the whole list for small businesses. */
export const CUSTOMER_SUGGESTIONS_SIZE = 50;
