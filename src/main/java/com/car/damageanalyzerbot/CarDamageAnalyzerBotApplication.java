package com.car.damageanalyzerbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CarDamageAnalyzerBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarDamageAnalyzerBotApplication.class, args);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}