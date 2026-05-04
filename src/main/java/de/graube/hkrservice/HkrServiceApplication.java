package de.graube.hkrservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startklasse der HKRService-Anwendung.
 */
@SpringBootApplication
public class HkrServiceApplication {

    /**
     * Startet den Spring-Boot-Kontext.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(HkrServiceApplication.class, args);
    }

}
