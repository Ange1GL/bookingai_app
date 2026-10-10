package com.github.angellariosacosta.bookingapp.application.port.in;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.result.CustomerListItem;

public interface SearchCustomersUseCase {
	List<CustomerListItem> search(String name, Long userId);
}
