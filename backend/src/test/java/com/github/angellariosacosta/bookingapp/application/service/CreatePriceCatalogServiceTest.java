package com.github.angellariosacosta.bookingapp.application.service;

import com.github.angellariosacosta.bookingapp.application.command.CreatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.port.out.PriceCatalogRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.DuplicatePriceCatalogLabelException;
import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePriceCatalogServiceTest {

    private final String labelTest = "Corte Barba";
    private final long priceTest = 150;
    private final Long userIdTest = 1L;
    private final Integer noExcludedId = null;

    @Mock
    private PriceCatalogRepositoryPort priceCatalogRepository;

    @InjectMocks
    private CreatePriceCatalogService service;

    private CreatePriceCatalogCommand validCommand() {
        return new CreatePriceCatalogCommand(labelTest, priceTest, userIdTest);
    }

    @DisplayName("Should save and return the price catalog when the label is not duplicated")
    @Test
    void shouldSaveAndReturnPriceCatalogWhenLabelIsNotDuplicated() {
        when(priceCatalogRepository.existsActiveByLabel(labelTest, userIdTest, noExcludedId)).thenReturn(false);
        when(priceCatalogRepository.save(any(PriceCatalog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceCatalog result = service.create(validCommand());

        assertNull(result.getId());
        assertTrue(result.isActive());
        assertEquals(labelTest, result.getLabel());
        assertEquals(priceTest, result.getPrice());
        assertEquals(userIdTest, result.getUserId());
    }

    @DisplayName("Should throw an exception and not save when the label already exists")
    @Test
    void shouldThrowAndNotSaveWhenLabelIsDuplicated() {
        when(priceCatalogRepository.existsActiveByLabel(labelTest, userIdTest, noExcludedId)).thenReturn(true);

        assertThrows(DuplicatePriceCatalogLabelException.class, () -> service.create(validCommand()));

        verify(priceCatalogRepository, never()).save(any(PriceCatalog.class));
    }

    @DisplayName("Should check duplicates by label and user without excluding any id")
    @Test
    void shouldCheckDuplicatesByLabelAndUserWithoutExcludingAnyId() {
        service.create(validCommand());

        verify(priceCatalogRepository).existsActiveByLabel(labelTest, userIdTest, noExcludedId);
    }

    @DisplayName("Should fail before touching the repository when the data is invalid")
    @Test
    void shouldFailBeforeTouchingRepositoryWhenDataIsInvalid() {
        CreatePriceCatalogCommand invalidCommand = new CreatePriceCatalogCommand(labelTest, 0, userIdTest);

        assertThrows(InvalidFieldException.class, () -> service.create(invalidCommand));

        verifyNoInteractions(priceCatalogRepository);
    }


}
