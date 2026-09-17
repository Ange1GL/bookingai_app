package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidStatusAppointmentExcepcion;

import lombok.Getter;

@Getter
public enum StatusAppointment {

    // PENDING -> La cita ha si creada pero no ha pasado
	PENDING(1, "PENDING"),

    // CANCELLED -> La cita ha sido cancelada
	CANCELLED(2, "CANCELLED"),

    // IN_PROGRESS -> La cita esta en curso
	IN_PROGRESS(3, "IN_PROGRESS"),

    // FINALIZED -> La cita ha finalizado
    FINALIZED(4, "FINALIZED");


    private final Integer id;
    private final String name;

    StatusAppointment(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public static StatusAppointment fromId(Integer id) {
        for (StatusAppointment status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        throw new InvalidStatusAppointmentExcepcion("Invalid status with id: " + id);
    }
    
}
