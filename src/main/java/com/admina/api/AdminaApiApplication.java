package com.admina.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminaApiApplication.class, args);
	}

}
