package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;

public interface CustomerBlacklistRepositoryPort {
	CustomerBlacklist save(CustomerBlacklist entry);
	boolean existsByCustomerId(Long customerId, Long userId);
	Optional<CustomerBlacklist> findByCustomerId(Long customerId, Long userId);
	// true si existía y se borró; false si el cliente no estaba en la lista negra.
	boolean deleteByCustomerId(Long customerId, Long userId);
	// Subconjunto de customerIds que están en lista negra: una sola consulta para una página entera.
	Set<Long> findBlacklistedIds(Collection<Long> customerIds, Long userId);
}
