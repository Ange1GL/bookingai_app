package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;


class PriceCatalogTest {

    private final long priceOne = 1;
    private final Long userIdTest = 1L;
    private final String fieldNamePrice =  "price";
    private final String labelPriceCatalogTest = "Corte Barba";
    private final String fieldNameLabel = "label";
    private final String fieldUserId = "userId";
    private final String fieldNameId = "id";
    private final Integer idTest = 5;
    private final long priceUpdated = 300;
    private final String labelUpdated = "Tinte";

    @DisplayName("Should show a exception when the price is invalid ")
    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void shouldShowAExceptionWhenThePriceIsInvalid(long price) {

        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> PriceCatalog.createNew(labelPriceCatalogTest, price, userIdTest));

        assertEquals(fieldNamePrice, ex.getField());
    }

    @DisplayName("Show a exception when label is null or blank")
    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldShowAExceptionWhenLabelIsNullOrBlank(String currentLabel) {

        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> PriceCatalog.createNew(currentLabel, priceOne, userIdTest));

        assertEquals(fieldNameLabel, ex.getField());

    }

    @DisplayName("Should throw an exception when label is null")
    @Test
    void shouldThrowWhenLabelIsNull() {
        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> PriceCatalog.createNew(null, priceOne, userIdTest));

        assertEquals(fieldNameLabel, ex.getField());
    }


    @DisplayName("Should throw an exception when userId is null")
    @Test
    void shouldThrowWhenUserIdIsNull() {
        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> PriceCatalog.createNew("Corte Barba", priceOne, null));

        assertEquals(fieldUserId, ex.getField());
    }

    @DisplayName("Should throw an exception when userId is zero or negative")
    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void shouldThrowWhenUserIdIsNotPositive(Long invalidUserId) {
        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> PriceCatalog.createNew("Corte Barba", priceOne, invalidUserId));

        assertEquals(fieldUserId, ex.getField());
    }

    @DisplayName("Should create an active price catalog without id when data is valid")
    @Test
    void shouldCreateActivePriceCatalogWhenDataIsValid() {
        PriceCatalog catalog = PriceCatalog.createNew(labelPriceCatalogTest, priceOne, userIdTest);

        assertNull(catalog.getId());
        assertTrue(catalog.isActive());
        assertEquals(labelPriceCatalogTest, catalog.getLabel());
        assertEquals(priceOne, catalog.getPrice());
        assertEquals(userIdTest, catalog.getUserId());
    }

    @DisplayName("Should throw an exception when update receives an invalid price")
    @Test
    void shouldThrowWhenUpdateReceivesInvalidPrice() {
        PriceCatalog catalog = PriceCatalog.createNew(labelPriceCatalogTest, priceOne, userIdTest);

        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> catalog.update(labelUpdated, 0));

        assertEquals(fieldNamePrice, ex.getField());
    }

    @DisplayName("Should keep id, userId and active and not modify the original when updating")
    @Test
    void shouldKeepIdentityAndNotMutateOriginalWhenUpdating() {
        PriceCatalog original = new PriceCatalog(idTest, priceOne, labelPriceCatalogTest, userIdTest, false);

        PriceCatalog updated = original.update(labelUpdated, priceUpdated);

        assertEquals(labelUpdated, updated.getLabel());
        assertEquals(priceUpdated, updated.getPrice());
        assertEquals(idTest, updated.getId());
        assertEquals(userIdTest, updated.getUserId());
        assertFalse(updated.isActive());

        assertNotSame(original, updated);
        assertEquals(labelPriceCatalogTest, original.getLabel());
        assertEquals(priceOne, original.getPrice());
    }

    @DisplayName("Should return an inactive copy and keep the original active when deactivating")
    @Test
    void shouldReturnInactiveCopyWhenDeactivating() {
        PriceCatalog original = new PriceCatalog(idTest, priceOne, labelPriceCatalogTest, userIdTest, true);

        PriceCatalog deactivated = original.deactivate();

        assertFalse(deactivated.isActive());
        assertEquals(idTest, deactivated.getId());
        assertEquals(labelPriceCatalogTest, deactivated.getLabel());
        assertEquals(priceOne, deactivated.getPrice());
        assertEquals(userIdTest, deactivated.getUserId());

        assertTrue(original.isActive());
    }

    @DisplayName("Should throw an exception when id is zero or negative")
    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void shouldThrowWhenIdIsNotPositive(Integer invalidId) {
        InvalidFieldException ex = assertThrows(
                InvalidFieldException.class,
                () -> new PriceCatalog(invalidId, priceOne, labelPriceCatalogTest, userIdTest, true));

        assertEquals(fieldNameId, ex.getField());
    }
}



