package com.studentfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudentFinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentFinanceApplication.class, args);
    }
}
