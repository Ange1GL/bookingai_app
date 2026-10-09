package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
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
import com.github.angellariosacosta.bookingapp.application.port.in.ListPriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.QueryAppointmentsUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.RescheduleAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.SearchCustomersUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CurrentUserPort;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

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
	private final ListPriceCatalogUseCase listPriceCatalog;
	private final CurrentUserPort currentUserPort;
	private final Clock clock;

	private static final Locale TOOL_LOCALE = Locale.of("es", "MX");

	@Tool(description = "Devuelve la fecha y hora actuales del negocio y el día de la semana. "
			+ "Úsala siempre que el usuario mencione fechas relativas (hoy, mañana, el viernes, en 2 horas).")
	public String getCurrentDateTime() {
		LocalDateTime now = LocalDateTime.now(clock);
		return "%s (%s, zona %s)".formatted(
				now.withNano(0), now.getDayOfWeek().getDisplayName(TextStyle.FULL, TOOL_LOCALE), clock.getZone());
	}

	@Tool(description = "Busca clientes por nombre. Devuelve lista de coincidencias parciales.")
	public List<CustomerSummary> searchCustomersByName(String name) {
		return searchCustomers.search(name, currentUserPort.getCurrentUserId()).stream()
				.map(CustomerSummary::from)
				.toList();
	}

	@Tool(description = "Lista los servicios del catálogo de precios (id, nombre y precio). "
			+ "Llámala antes de agendar para preguntar al usuario qué servicio aplicar.")
	public List<PriceCatalogSummary> listPriceCatalog() {
		return listPriceCatalog.list(currentUserPort.getCurrentUserId()).stream()
				.map(PriceCatalogSummary::from)
				.toList();
	}

	@Tool(description = "Registra una cita con un servicio del catálogo y crea al cliente si no existe. Busca al cliente por teléfono primero.")
	public AppointmentSummary bookAppointmentForCustomer(String name, String phone, String startTime, String endTime, Integer priceCatalogId) {
		BookAppointmentCommand command = new BookAppointmentCommand(
				name, phone, parseDateTime(startTime), parseDateTime(endTime), currentUserPort.getCurrentUserId(), priceCatalogId);
		return AppointmentSummary.from(bookAppointment.book(command));
	}

	@Tool(description = "Registra una cita con un servicio del catálogo para un cliente existente dado su id.")
	public AppointmentSummary createAppointmentForExistingCustomer(Long customerId, String startTime, String endTime, Integer priceCatalogId) {
		CreateAppointmentCommand command = new CreateAppointmentCommand(
				parseDateTime(startTime), parseDateTime(endTime), customerId, currentUserPort.getCurrentUserId(), priceCatalogId);
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
		return queryAppointments.findByCustomer(customerId, currentUserPort.getCurrentUserId()).stream()
				.map(AppointmentSummary::from)
				.toList();
	}

	@Tool(description = "Devuelve citas que cubren un horario específico. Fecha en formato yyyy-MM-dd, hora en formato HH:mm.")
	public List<AppointmentSummary> getAppointmentsByTimeSlot(String date, String hour) {
		LocalDate localDate = LocalDate.parse(date);
		LocalTime localTime = LocalTime.parse(hour);
		return queryAppointments.findByTimeSlot(currentUserPort.getCurrentUserId(), localDate, localTime).stream()
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

	public record PriceCatalogSummary(Integer id, String label, Long price) {
		static PriceCatalogSummary from(PriceCatalog p) {
			return new PriceCatalogSummary(p.getId(), p.getLabel(), p.getPrice());
		}
	}

	public record AppointmentSummary(Long id, String startTime, String endTime, String status, Long customerId, String customerName, Long userId,
			Integer priceCatalogId, String serviceLabel, Long price) {
		static AppointmentSummary from(Appointment a) {
			return new AppointmentSummary(
					a.getId(),
					a.getStartTime().toString(),
					a.getEndTime().toString(),
					a.getStatus().getName(),
					a.getCustomer().getId(),
					a.getCustomer().getName(),
						a.getUserId(),
						a.getPriceCatalog().getId(),
						a.getPriceCatalog().getLabel(),
						a.getPriceCatalog().getPrice()
			);
		}
	}
}
