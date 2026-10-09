package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.query.ListCustomersQuery;
import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;

public interface ListCustomersUseCase {
	PageResult<CustomerListItem> list(ListCustomersQuery query);
}
