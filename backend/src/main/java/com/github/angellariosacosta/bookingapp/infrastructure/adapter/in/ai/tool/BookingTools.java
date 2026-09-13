package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.command.CancelAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.command.CreateAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.command.RescheduleAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CancelAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CreateAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RescheduleAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.SearchCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CurrentUserPort;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingTools {

	private final SearchCustomersUseCase searchCustomers;
	private final BookAppointmentUseCase bookAppointment;
	private final CreateAppointmentUseCase createAppointment;
	private final CancelAppointmentUseCase cancelAppointment;
	private final RescheduleAppointmentUseCase rescheduleAppointment;
	private final QueryAppointmentsUseCase queryAppointments;
	private final CurrentUserPort currentUserPort;

	@Tool(description = "Busca clientes por nombre. Devuelve lista de coincidencias parciales.")
	public List<CustomerSummary> searchCustomersByName(String name) {
		return searchCustomers.search(name).stream()
				.map(CustomerSummary::from)
				.toList();
	}

	@Tool(description = "Registra una cita y crea al cliente si no existe. Busca al cliente por teléfono primero.")
	public AppointmentSummary bookAppointmentForCustomer(String name, String phone, String startTime, String endTime) {
		BookAppointmentCommand command = new BookAppointmentCommand(
				name, phone, parseDateTime(startTime), parseDateTime(endTime), currentUserPort.getCurrentUserId());
		return AppointmentSummary.from(bookAppointment.book(command));
	}

	@Tool(description = "Registra una cita para un cliente existente dado su id.")
	public AppointmentSummary createAppointmentForExistingCustomer(Long customerId, String startTime, String endTime) {
		CreateAppointmentCommand command = new CreateAppointmentCommand(
				parseDateTime(startTime), parseDateTime(endTime), customerId, currentUserPort.getCurrentUserId());
		return AppointmentSummary.from(createAppointment.create(command));
	}

	@Tool(description = "Cancela una cita dado su id.")
	public AppointmentSummary cancelAppointmentById(Long appointmentId) {
		CancelAppointmentCommand command = new CancelAppointmentCommand(appointmentId, currentUserPort.getCurrentUserId());
		return AppointmentSummary.from(cancelAppointment.cancel(command));
	}

	@Tool(description = "Mueve una cita a un nuevo horario. Verifica disponibilidad antes de mover.")
	public AppointmentSummary rescheduleAppointmentById(Long appointmentId, String newStartTime, String newEndTime) {
		RescheduleAppointmentCommand command = new RescheduleAppointmentCommand(
				appointmentId, parseDateTime(newStartTime), parseDateTime(newEndTime), currentUserPort.getCurrentUserId());
		return AppointmentSummary.from(rescheduleAppointment.reschedule(command));
	}

	@Tool(description = "Lista citas activas de un cliente por su id.")
	public List<AppointmentSummary> getAppointmentsForCustomer(Long customerId) {
		return queryAppointments.findByCustomer(customerId).stream()
				.map(AppointmentSummary::from)
				.toList();
	}

	@Tool(description = "Devuelve citas que cubren un horario específico. Fecha en formato yyyy-MM-dd, hora en formato HH:mm.")
	public List<AppointmentSummary> getAppointmentsByTimeSlot(String date, String hour) {
		LocalDate localDate = LocalDate.parse(date);
		LocalTime localTime = LocalTime.parse(hour);
		return queryAppointments.findByTimeSlot(localDate, localTime).stream()
				.map(AppointmentSummary::from)
				.toList();
	}

	private LocalDateTime parseDateTime(String isoDateTime) {
		return LocalDateTime.parse(isoDateTime);
	}

	public record CustomerSummary(Long id, String name, String phone) {
		static CustomerSummary from(Customer c) {
			return new CustomerSummary(c.getId(), c.getName(), c.getPhone());
		}
	}

	public record AppointmentSummary(Long id, String startTime, String endTime, String status, Long customerId, String customerName, Long userId) {
		static AppointmentSummary from(Appointment a) {
			return new AppointmentSummary(
					a.getId(),
					a.getStartTime().toString(),
					a.getEndTime().toString(),
					a.getStatus().getName(),
					a.getCustomer().getId(),
					a.getCustomer().getName(),
					a.getUserId()
			);
		}
	}
}
