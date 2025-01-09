package com.example.checkfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.checkfile")
public class CheckfileApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckfileApplication.class, args);
	}

}
