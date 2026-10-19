package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface ListUsersUseCase {

	PageResult<User> list(ListUsersQuery query);
}
