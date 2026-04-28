package com.cableops.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling                // ← add this
public class FiblinkCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(FiblinkCrmApplication.class, args);
	}

}
