package com.Ecomarket.sistemareportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SistemareportesApplication {
	public static void main(String[] args) {
		SpringApplication.run(SistemareportesApplication.class, args);
	}
}
