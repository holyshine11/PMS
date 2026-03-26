package com.hola;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HolaPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HolaPmsApplication.class, args);
    }
}
