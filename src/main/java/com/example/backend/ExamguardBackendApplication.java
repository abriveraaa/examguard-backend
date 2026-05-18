package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@SpringBootApplication
@EnableAsync
public class ExamguardBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamguardBackendApplication.class, args);
    }

}
