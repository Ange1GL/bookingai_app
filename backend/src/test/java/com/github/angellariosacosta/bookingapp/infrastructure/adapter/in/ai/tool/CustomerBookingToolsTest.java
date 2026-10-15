package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import com.github.angellariosacosta.bookingapp.application.command.BookAppointmentCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.BookAppointmentUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.CompanySelectionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.FindCompanyUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.FindCustomerByPhoneUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListPriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool.CustomerBookingTools.BookingConfirmation;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.ai.tool.CustomerBookingTools.CustomerProfile;

@ExtendWith(MockitoExtension.class)
class CustomerBookingToolsTest {

	private static final String KEY = "1001:5215512345678";
	private static final String PHONE = "5215512345678";
	private static final Long OWNER_ID = 7L;
	private static final Long OTHER_OWNER_ID = 8L;
	private static final CompanySelection SELECTION = new CompanySelection(10L, "Barbería El Güero", OWNER_ID);
	private static final String START = "2030-01-01T10:00:00";
	private static final String END = "2030-01-01T10:30:00";

	@Mock
	private FindCompanyUseCase findCompany;
	@Mock
	private CompanySelectionUseCase companySelection;
	@Mock
	private FindCustomerByPhoneUseCase findCustomerByPhone;
	@Mock
	private ListPriceCatalogUseCase listPriceCatalog;
	@Mock
	private BookAppointmentUseCase bookAppointment;

	private CustomerBookingTools tools;
	private final ToolContext context = new ToolContext(Map.of(
			CustomerBookingTools.CONVERSATION_KEY, KEY, CustomerBookingTools.CUSTOMER_PHONE, PHONE));

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);
		tools = new CustomerBookingTools(findCompany, companySelection, findCustomerByPhone, listPriceCatalog,
				bookAppointment, clock);
	}

	private Appointment appointmentFor(Customer customer, PriceCatalog service) {
		return Appointment.createNew(LocalDateTime.parse(START), LocalDateTime.parse(END), customer, OWNER_ID, service,
				LocalDateTime.of(2026, 10, 14, 12, 0));
	}

	@DisplayName("Should book under the owner from the server state and the phone from the context")
	@Test
	void shouldBookUsingStateOwnerAndContextPhone() {
		PriceCatalog service = new PriceCatalog(3, 150, "Corte", OWNER_ID, true);
		Customer customer = Customer.builder().id(5L).name("Ana").phone(PHONE).userId(OWNER_ID).build();
		when(companySelection.current(KEY)).thenReturn(Optional.of(SELECTION));
		when(findCustomerByPhone.find(PHONE, OWNER_ID)).thenReturn(Optional.of(customer));
		when(bookAppointment.book(any(BookAppointmentCommand.class))).thenReturn(appointmentFor(customer, service));

		BookingConfirmation confirmation = tools.bookMyAppointment(null, START, END, 3, context);

		ArgumentCaptor<BookAppointmentCommand> captor = ArgumentCaptor.forClass(BookAppointmentCommand.class);
		verify(bookAppointment).book(captor.capture());
		assertEquals(OWNER_ID, captor.getValue().userId());
		assertEquals(PHONE, captor.getValue().phone());
		assertEquals(3, captor.getValue().priceCatalogId());
		assertEquals("Barbería El Güero", confirmation.company());
		assertEquals("Corte", confirmation.service());
	}

	@DisplayName("Should require a name when the customer is new")
	@Test
	void shouldRequireNameForNewCustomer() {
		when(companySelection.current(KEY)).thenReturn(Optional.of(SELECTION));
		when(findCustomerByPhone.find(PHONE, OWNER_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> tools.bookMyAppointment(" ", START, END, 3, context));

		verify(bookAppointment, never()).book(any());
	}

	@DisplayName("Should refuse to book, list services or read the profile before a company is selected")
	@Test
	void shouldRequireSelectedCompany() {
		when(companySelection.current(KEY)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> tools.bookMyAppointment("Ana", START, END, 3, context));
		assertThrows(IllegalStateException.class, () -> tools.listServices(context));
		assertThrows(IllegalStateException.class, () -> tools.getMyProfile(context));

		verifyNoInteractions(bookAppointment, listPriceCatalog, findCustomerByPhone);
	}

	@DisplayName("Should list only the services of the selected company owner")
	@Test
	void shouldListServicesOfSelectedOwner() {
		when(companySelection.current(KEY)).thenReturn(Optional.of(SELECTION));
		when(listPriceCatalog.list(OWNER_ID)).thenReturn(List.of(new PriceCatalog(3, 150, "Corte", OWNER_ID, true)));

		assertEquals(1, tools.listServices(context).size());
		verify(listPriceCatalog, never()).list(OTHER_OWNER_ID);
	}

	@DisplayName("Should identify a returning customer by the WhatsApp number")
	@Test
	void shouldIdentifyReturningCustomer() {
		when(companySelection.current(KEY)).thenReturn(Optional.of(SELECTION));
		when(findCustomerByPhone.find(PHONE, OWNER_ID))
				.thenReturn(Optional.of(Customer.builder().id(5L).name("Ana").phone(PHONE).userId(OWNER_ID).build()));

		CustomerProfile profile = tools.getMyProfile(context);

		assertTrue(profile.known());
		assertEquals("Ana", profile.name());
	}

	@DisplayName("Should report an unknown customer")
	@Test
	void shouldReportUnknownCustomer() {
		when(companySelection.current(KEY)).thenReturn(Optional.of(SELECTION));
		when(findCustomerByPhone.find(PHONE, OWNER_ID)).thenReturn(Optional.empty());

		assertFalse(tools.getMyProfile(context).known());
	}
}
