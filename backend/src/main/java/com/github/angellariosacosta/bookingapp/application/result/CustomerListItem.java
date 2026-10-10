package com.github.angellariosacosta.bookingapp.application.result;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;

public record CustomerListItem(Customer customer, boolean blacklisted) {
}
