package com.github.angellariosacosta.bookingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AglacitaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AglacitaApplication.class, args);
	}

}
