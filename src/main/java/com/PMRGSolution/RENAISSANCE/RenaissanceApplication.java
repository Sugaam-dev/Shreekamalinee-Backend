package com.PMRGSolution.RENAISSANCE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RenaissanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RenaissanceApplication.class, args);
	}

}
