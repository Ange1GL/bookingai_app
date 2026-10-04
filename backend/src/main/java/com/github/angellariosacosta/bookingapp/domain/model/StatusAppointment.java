package com.github.angellariosacosta.bookingapp.domain.model;

import com.github.angellariosacosta.bookingapp.domain.exception.InvalidStatusAppointmentExcepcion;

import lombok.Getter;

@Getter
public enum StatusAppointment {

    // RESERVED -> La cita fue creada y esta vigente. "En curso" y "terminada" no se guardan:
    // se derivan de startTime/endTime y la hora actual.
	RESERVED(1, "RESERVED"),

    // CANCELLED -> La cita ha sido cancelada; libera el horario en el anti-empalme
	CANCELLED(2, "CANCELLED");


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
