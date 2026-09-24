package com.checkout.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CheckOutApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckOutApplication.class, args);
    }

}
