package ru.haritonenko.eventnotificator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ru.haritonenko")
public class EventNotificatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventNotificatorApplication.class, args);
    }

}
