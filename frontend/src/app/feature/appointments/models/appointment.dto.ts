/** Mirrors the backend AppointmentResponse record. Dates are local date-times without offset. */
export interface AppointmentDto {
  id: number;
  startTime: string;
  endTime: string;
  customerId: number;
  customerName: string;
  status: string;
  statusId: number;
}

/** Persisted statuses (backend StatusAppointment). */
export const AppointmentStatusId = {
  Reserved: 1,
  Cancelled: 2,
} as const;
