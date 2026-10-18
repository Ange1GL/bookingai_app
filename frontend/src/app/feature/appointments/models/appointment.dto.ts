/** Mirrors the backend AppointmentResponse record. Dates are local date-times without offset. */
export interface AppointmentDto {
  id: number;
  startTime: string;
  endTime: string;
  customerId: number;
  customerName: string;
  status: string;
  statusId: number;
  priceCatalogId: number;
  serviceLabel: string;
  price: number;
}

/** Mirrors CreateAppointmentRequest. Dates are `YYYY-MM-DDTHH:mm:ss` in local time, no offset. */
export interface CreateAppointmentRequestDto {
  customerId: number;
  startTime: string;
  endTime: string;
  priceCatalogId: number;
}

/** Persisted statuses (backend StatusAppointment). */
export const AppointmentStatusId = {
  Reserved: 1,
  Cancelled: 2,
} as const;
