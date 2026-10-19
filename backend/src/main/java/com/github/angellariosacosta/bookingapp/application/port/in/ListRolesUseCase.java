package com.github.angellariosacosta.bookingapp.application.port.in;

import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface ListRolesUseCase {

	PageResult<Role> list(ListRolesQuery query);
}
