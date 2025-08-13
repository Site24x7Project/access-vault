package com.accessvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ ADD THIS

@SpringBootApplication
@EnableScheduling // ✅ ADD THIS
public class AccessvaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessvaultApplication.class, args);
    }
}
