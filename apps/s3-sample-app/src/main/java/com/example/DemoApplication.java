package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.Cloud;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
	
	@Bean
	public CommandLineRunner printCloudServices(Cloud cloud) {
		return (args) -> {
			System.out.println("=======  :" + cloud.getCloudProperties());
		};
	}
	
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}



