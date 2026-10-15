package com.github.angellariosacosta.bookingapp.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.port.in.CompanySelectionUseCase;
import com.github.angellariosacosta.bookingapp.application.port.out.CompanyRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.port.out.CustomerConversationPort;
import com.github.angellariosacosta.bookingapp.application.result.CompanySelection;
import com.github.angellariosacosta.bookingapp.domain.exception.CompanyNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Company;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanySelectionService implements CompanySelectionUseCase {

	private final CompanyRepositoryPort companyRepository;
	private final CustomerConversationPort conversations;

	@Override
	@Transactional(readOnly = true)
	public CompanySelection select(String conversationKey, Long ownerUserId) {
		Company company = companyRepository.findByUserId(ownerUserId)
				.orElseThrow(() -> new CompanyNotFoundException("Company not found for owner: " + ownerUserId));
		CompanySelection selection = new CompanySelection(company.getUserId(), company.getName());
		conversations.saveSelection(conversationKey, selection);
		return selection;
	}

	@Override
	public Optional<CompanySelection> current(String conversationKey) {
		return conversations.findSelection(conversationKey);
	}
}
