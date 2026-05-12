package com.ssafy.modu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ModuApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModuApplication.class, args);
	}

}
