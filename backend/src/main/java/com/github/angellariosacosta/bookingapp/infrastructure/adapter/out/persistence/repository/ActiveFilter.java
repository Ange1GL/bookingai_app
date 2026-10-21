package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

/**
 * Filtro por estado para consultas paginadas. Es un entero (nunca null) porque Postgres no puede tipar un
 * parámetro null en {@code :x is null}; las consultas lo comparan con 0/1/2.
 */
public final class ActiveFilter {

    public static final int ANY = 0;
    public static final int ONLY_ACTIVE = 1;
    public static final int ONLY_INACTIVE = 2;

    private ActiveFilter() {
    }

    public static int of(Boolean active) {
        if (active == null) {
            return ANY;
        }
        return active ? ONLY_ACTIVE : ONLY_INACTIVE;
    }
}
