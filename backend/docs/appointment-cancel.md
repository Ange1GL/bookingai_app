# Cancelar cita

`POST /api/v1/appointments/{id}/cancel` (autenticado, sin cuerpo).

`AppointmentController` llama al puerto `CancelAppointmentUseCase` (implementado por `CancelAppointmentService`) con `CancelAppointmentCommand(id, userId)`; el `userId` sale del token (`@CurrentUserId`), así que solo se pueden cancelar citas del propio tenant.

- 200: la cita con `status` `CANCELLED` (mismo `AppointmentResponse` que el resto).
- 404: la cita no existe o es de otro tenant (`AppointmentNotFoundException`).
- 409/error de transición: la cita no está `RESERVED` (`InvalidStatusTransitionException`, vía `GlobalExceptionHandler`).
