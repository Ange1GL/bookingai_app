package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CompanySelectionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.FindCompanyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.FindCustomerByPhoneUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListPriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;

import lombok.RequiredArgsConstructor;

/**
 * Herramientas del agente para clientes finales de WhatsApp. A diferencia de {@link BookingTools} no hay usuario
 * autenticado: el tenant (dueño del negocio) sale del estado del servidor y el teléfono del cliente, de la firma HMAC
 * del webhook; ambos llegan por {@link ToolContext}, nunca como parámetros que pueda inventar el modelo.
 * Solo expone reservar: sin lista negra, sin buscar a otros clientes ni tocar citas ajenas.
 */
@Component
@RequiredArgsConstructor
public class CustomerBookingTools {

	public static final String CONVERSATION_KEY = "conversationKey";
	public static final String CUSTOMER_PHONE = "customerPhone";

	private static final Locale TOOL_LOCALE = Locale.of("es", "MX");
	private static final String NO_COMPANY_SELECTED = "Primero hay que elegir un negocio con la herramienta selectCompany";
	private static final String NAME_REQUIRED = "El cliente es nuevo: pídele su nombre completo antes de agendar";

	private final FindCompanyUseCase findCompany;
	private final CompanySelectionUseCase companySelection;
	private final FindCustomerByPhoneUseCase findCustomerByPhone;
	private final ListPriceCatalogUseCase listPriceCatalog;
	private final BookAppointmentUseCase bookAppointment;
	private final Clock clock;

	@Tool(description = "Devuelve la fecha y hora actuales del negocio y el día de la semana. "
			+ "Úsala siempre que el cliente mencione fechas relativas (hoy, mañana, el viernes, en 2 horas).")
	public String getCurrentDateTime() {
		LocalDateTime now = LocalDateTime.now(clock);
		return "%s (%s, zona %s)".formatted(
				now.withNano(0), now.getDayOfWeek().getDisplayName(TextStyle.FULL, TOOL_LOCALE), clock.getZone());
	}

	@Tool(description = "Busca negocios registrados por el nombre del negocio (coincidencia parcial, sin distinguir "
			+ "mayúsculas). Devuelve businessId, nombre del negocio y nombre del dueño. "
			+ "Lista vacía significa que ese negocio no está registrado en la plataforma.")
	public List<CompanySummary> findCompanyByName(String businessName) {
		return findCompany.searchByName(businessName).stream().map(CompanySummary::from).toList();
	}

	@Tool(description = "Busca negocios por el nombre de su dueño (coincidencia parcial, sin distinguir mayúsculas). "
			+ "Úsala cuando el cliente dice el nombre de la persona y no el del negocio. Puede devolver varios dueños con "
			+ "el mismo nombre: en ese caso muéstralos con el nombre de su negocio y pide al cliente que elija. "
			+ "Lista vacía significa que no hay un dueño con ese nombre registrado en la plataforma.")
	public List<CompanySummary> findCompanyByOwnerName(String ownerName) {
		return findCompany.searchByOwnerName(ownerName).stream().map(CompanySummary::from).toList();
	}

	@Tool(description = "Elige el negocio donde el cliente quiere agendar, por su businessId (obtenido de "
			+ "findCompanyByName o findCompanyByOwnerName). Llámala solo cuando el cliente confirmó cuál negocio es.")
	public SelectedCompany selectCompany(Long businessId, ToolContext toolContext) {
		CompanySelection selection = companySelection.select(conversationKey(toolContext), businessId);
		return new SelectedCompany(selection.ownerUserId(), selection.companyName());
	}

	@Tool(description = "Indica si el cliente que escribe ya está registrado en el negocio elegido (se identifica por su "
			+ "número de WhatsApp) y su nombre. Si known es false, pídele su nombre antes de agendar.")
	public CustomerProfile getMyProfile(ToolContext toolContext) {
		CompanySelection selection = requireSelection(toolContext);
		return findCustomerByPhone.find(customerPhone(toolContext), selection.ownerUserId())
				.map(CustomerProfile::known)
				.orElseGet(CustomerProfile::unknown);
	}

	@Tool(description = "Lista los servicios del negocio elegido (id, nombre y precio). "
			+ "Llámala antes de agendar para preguntar al cliente qué servicio quiere.")
	public List<ServiceSummary> listServices(ToolContext toolContext) {
		CompanySelection selection = requireSelection(toolContext);
		return listPriceCatalog.list(selection.ownerUserId()).stream().map(ServiceSummary::from).toList();
	}

	@Tool(description = "Agenda una cita para el cliente que escribe en el negocio elegido, con un servicio de listServices. "
			+ "Fechas en formato ISO yyyy-MM-ddTHH:mm:ss. customerName solo es obligatorio si el cliente es nuevo. "
			+ "Úsala solo tras confirmar con el cliente servicio, día y hora.")
	public BookingConfirmation bookMyAppointment(String customerName, String startTime, String endTime,
			Integer priceCatalogId, ToolContext toolContext) {
		CompanySelection selection = requireSelection(toolContext);
		String phone = customerPhone(toolContext);
		if (findCustomerByPhone.find(phone, selection.ownerUserId()).isEmpty() && isBlank(customerName)) {
			throw new IllegalArgumentException(NAME_REQUIRED);
		}
		Appointment appointment = bookAppointment.book(new BookAppointmentCommand(
				customerName, phone, LocalDateTime.parse(startTime), LocalDateTime.parse(endTime),
				selection.ownerUserId(), priceCatalogId));
		return BookingConfirmation.from(appointment, selection.companyName());
	}

	private CompanySelection requireSelection(ToolContext toolContext) {
		return companySelection.current(conversationKey(toolContext))
				.orElseThrow(() -> new IllegalStateException(NO_COMPANY_SELECTED));
	}

	private static String conversationKey(ToolContext toolContext) {
		return (String) toolContext.getContext().get(CONVERSATION_KEY);
	}

	private static String customerPhone(ToolContext toolContext) {
		return (String) toolContext.getContext().get(CUSTOMER_PHONE);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	// businessId es el id del dueño del negocio (relación uno a uno).
	public record CompanySummary(Long businessId, String businessName, String ownerName) {
		static CompanySummary from(CompanySearchResult result) {
			return new CompanySummary(result.ownerUserId(), result.companyName(), result.ownerName());
		}
	}

	public record SelectedCompany(Long businessId, String businessName) {
	}

	public record CustomerProfile(boolean known, String name) {
		static CustomerProfile known(Customer customer) {
			return new CustomerProfile(true, customer.getName());
		}

		static CustomerProfile unknown() {
			return new CustomerProfile(false, null);
		}
	}

	public record ServiceSummary(Integer id, String label, Long price) {
		static ServiceSummary from(PriceCatalog service) {
			return new ServiceSummary(service.getId(), service.getLabel(), service.getPrice());
		}
	}

	// Solo lo que el cliente necesita: sin ids internos ni el userId del dueño.
	public record BookingConfirmation(String company, String startTime, String endTime, String service, Long price,
			String status) {
		static BookingConfirmation from(Appointment appointment, String companyName) {
			return new BookingConfirmation(
					companyName,
					appointment.getStartTime().toString(),
					appointment.getEndTime().toString(),
					appointment.getPriceCatalog().getLabel(),
					appointment.getPriceCatalog().getPrice(),
					appointment.getStatus().getName());
		}
	}
}
