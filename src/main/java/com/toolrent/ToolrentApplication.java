package com.toolrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ToolrentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToolrentApplication.class, args);
        System.out.println("✅ Application is running!");
	}

}
