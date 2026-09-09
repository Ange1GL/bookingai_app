package com.github.angellariosacosta.bookingapp.domain.model;


import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Getter
public class Appointment {

	@Setter
	private Long id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Customer customer; 
	private StatusAppointment status;
	
	
	public static Appointment createNew(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer
			) {
		return new Appointment(startTime, endTime, customer, StatusAppointment.IN_PROGRESS);
	}
	
	
	public Appointment(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Customer customer,
			StatusAppointment status
			) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.customer = customer;
		this.status = status;
	}
	
}
