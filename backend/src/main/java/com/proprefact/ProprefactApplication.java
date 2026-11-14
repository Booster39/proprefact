package com.proprefact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProprefactApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProprefactApplication.class, args);
    }
}
