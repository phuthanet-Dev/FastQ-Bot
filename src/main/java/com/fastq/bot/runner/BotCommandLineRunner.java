package com.fastq.bot.runner;

import com.fastq.bot.service.QueueAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Auto-starts the queue booking automation when the application boots.
 * <p>
 * Implements {@link CommandLineRunner} so that {@code processAllAccounts()}
 * is invoked immediately after Spring context initialization.
 *
 * <h3>Usage</h3>
 * <ol>
 *   <li>Insert account rows into the {@code accounts} table via SQL or an API</li>
 *   <li>Start the application: {@code mvn spring-boot:run}</li>
 *   <li>The runner will automatically pick up all non-BOOKED accounts and start processing</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotCommandLineRunner implements CommandLineRunner {

    private final QueueAutomationService queueAutomationService;

    @Override
    public void run(String... args) {
        log.info("+-------------------------------------+");
        log.info("|     FastQ-Bot v1.0.0 Starting...    |");
        log.info("|     Java 21 Virtual Threads         |");
        log.info("|     1 Account = 1 Target Shop       |");
        log.info("+-------------------------------------+");

        queueAutomationService.processAllAccounts();
    }
}
