package com.ev.charging.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChargingApiToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingApiToolApplication.class, args);
    }
}
