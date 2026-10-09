package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidFieldException;
import com.github.angellariosacosta.bookingapp.domain.exception.PriceCatalogNotEditableException;
import lombok.Getter;

@Getter
public class PriceCatalog {

    // null mientras el servicio no se ha guardado
    private final Integer id;
    private final long price;
    private final String label;
    // dueño del servicio: cada usuario tiene su propio catalogo
    private final Long userId;
    private final boolean active;

    public PriceCatalog(Integer id, long price, String label, Long userId, boolean active) {
        if (id != null && id <= 0) {
            throw new InvalidFieldException("id", "must be greater than 0");
        }
        if (price <= 0) {
            throw new InvalidFieldException("price", "must be greater than 0");
        }
        if (label == null || label.isBlank()) {
            throw new InvalidFieldException("label", "must not be null or blank");
        }
        this.id = id;
        this.price = price;
        this.label = label;
        this.userId = userId;
        this.active = active;
    }

    public static PriceCatalog createNew(String label, long price, Long userId) {
        if (userId == null) {
            throw new InvalidFieldException("userId", "must not be null");
        }
        return new PriceCatalog(null, price, label, userId, true);
    }

    public PriceCatalog update(String newLabel, long newPrice) {
        return new PriceCatalog(id, newPrice, newLabel, userId, active);
    }

    public PriceCatalog deactivate() {
        return new PriceCatalog(id, price, label, userId, false);
    }

    // Los servicios de otros usuarios no se pueden modificar.
    public void ensureOwnedBy(Long requesterId) {
        if (userId == null || !userId.equals(requesterId)) {
            throw new PriceCatalogNotEditableException("Price catalog " + id + " cannot be modified");
        }
    }
}
