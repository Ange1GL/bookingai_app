package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public interface ListCustomersUseCase {
	PageResult<Customer> list(ListCustomersQuery query);
}
