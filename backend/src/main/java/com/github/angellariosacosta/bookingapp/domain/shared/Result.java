package com.github.angellariosacosta.bookingapp.domain.shared;

import java.util.Objects;

public final class Result<T, E> {

    private final T value;
    private final E error;

    private Result(T value, E error) {
        this.value = value;
        this.error = error;
    }

    /* ========= FACTORIES ========= */

    public static <T, E> Result<T, E> success(T value) {
        Objects.requireNonNull(value, "value must not be null");
        return new Result<>(value, null);
    }

    public static <T, E> Result<T, E> failure(E error) {
        Objects.requireNonNull(error, "error must not be null");
        return new Result<>(null, error);
    }

    /* ========= API ========= */

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public T getValue() {
        if (isFailure()) {
            throw new IllegalStateException("Cannot get value from failure result");
        }
        return value;
    }

    public E getError() {
        if (isSuccess()) {
            throw new IllegalStateException("Cannot get error from success result");
        }
        return error;
    }
}
