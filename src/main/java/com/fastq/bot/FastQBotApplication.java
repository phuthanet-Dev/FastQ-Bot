package com.fastq.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FastQ-Bot: Automated Queue Booking System
 * <p>
 * Uses Java 21 Virtual Threads for lightweight concurrent multi-account processing.
 * Orchestrates reverse-engineered QueQ API calls to automate restaurant queue bookings.
 */
@SpringBootApplication
public class FastQBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastQBotApplication.class, args);
    }
}
