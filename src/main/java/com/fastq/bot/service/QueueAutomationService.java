package com.fastq.bot.service;

import com.fastq.bot.client.QueQApiClient;
import com.fastq.bot.client.QueQApiClient.HttpResponseException;
import com.fastq.bot.dto.*;
import com.fastq.bot.entity.AccountEntity;
import com.fastq.bot.enums.AccountStatus;
import com.fastq.bot.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Core orchestration service that drives the automated queue booking workflow.
 * <p>
 * For each eligible account, the workflow is:
 * <ol>
 *   <li><b>Register</b> (if {@code is_registered == false})</li>
 *   <li><b>Login</b> → retrieve {@code user_token}</li>
 *   <li><b>Accept PDPA</b> (if {@code is_pdpa_accepted == false})</li>
 *   <li><b>Monitor Queue</b> → poll until {@code current_queue > 0 AND <= threshold}</li>
 *   <li><b>Anti-Fraud</b> → submit device integrity check</li>
 *   <li><b>Submit Queue</b> → book the queue</li>
 * </ol>
 *
 * <h3>Concurrency Model</h3>
 * Uses Java 21 Virtual Threads ({@code Executors.newVirtualThreadPerTaskExecutor()})
 * so each account runs in its own lightweight thread. This enables processing
 * hundreds of accounts concurrently without a large thread pool.
 *
 * <h3>Error Handling</h3>
 * <ul>
 *   <li><b>HTTP 5xx</b> → Retry up to 3 times with backoff (5s, 10s, 15s)</li>
 *   <li><b>HTTP 401</b> → Re-login to refresh the user_token</li>
 *   <li><b>HTTP 403</b> → KILL SWITCH: mark account as ERROR, stop immediately</li>
 *   <li><b>Other errors</b> → Mark account as ERROR</li>
 * </ul>
 */
@Slf4j
@Service
public class QueueAutomationService {

    private final QueQApiClient apiClient;
    private final AccountRepository accountRepository;
    private final ExecutorService virtualThreadExecutor;

    @Value("${bot.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${bot.poll-interval-seconds:10}")
    private int pollIntervalSeconds;

    public QueueAutomationService(QueQApiClient apiClient,
                                   AccountRepository accountRepository,
                                   @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.apiClient = apiClient;
        this.accountRepository = accountRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    // ══════════════════════════════════════════════
    // PUBLIC ENTRY POINT
    // ══════════════════════════════════════════════

    /**
     * Loads all non-BOOKED accounts and processes each one in a Virtual Thread.
     * Blocks until all accounts are done (or errored).
     */
    public void processAllAccounts() {
        List<AccountEntity> accounts = accountRepository.findAllByStatusNot(AccountStatus.BOOKED);

        if (accounts.isEmpty()) {
            log.warn("⚠ No eligible accounts found (all BOOKED or none exist). Exiting.");
            return;
        }

        log.info("═══════════════════════════════════════════");
        log.info("  FastQ-Bot Starting: {} account(s) to process", accounts.size());
        log.info("═══════════════════════════════════════════");

        CountDownLatch latch = new CountDownLatch(accounts.size());

        for (AccountEntity account : accounts) {
            virtualThreadExecutor.submit(() -> {
                try {
                    processAccount(account);
                } catch (Exception e) {
                    log.error("💀 Unhandled exception for account [{}]: {}",
                            account.getEmail(), e.getMessage(), e);
                    markError(account, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Main thread interrupted while waiting for accounts to complete");
        }

        log.info("═══════════════════════════════════════════");
        log.info("  FastQ-Bot Finished: All accounts processed");
        log.info("═══════════════════════════════════════════");
    }

    // ══════════════════════════════════════════════
    // SINGLE ACCOUNT WORKFLOW
    // ══════════════════════════════════════════════

    /**
     * Executes the full booking workflow for a single account.
     */
    private void processAccount(AccountEntity account) throws Exception {
        String email = account.getEmail();
        log.info("──────────────────────────────────────");
        log.info("▶ Processing account: {}", email);
        log.info("──────────────────────────────────────");

        // Update status to WAITING
        account.setStatus(AccountStatus.WAITING);
        accountRepository.save(account);

        // ── Step 1: Registration (conditional) ──
        if (!Boolean.TRUE.equals(account.getIsRegistered())) {
            log.info("[{}] Step 1: Registering new account...", email);
            executeWithRetry(() -> {
                apiClient.signUp(
                        account.getEmail(),
                        account.getAccountName(),
                        account.getPassword(),
                        account.getProxyHost(),
                        account.getProxyPort()
                );
                return null;
            }, account, "SignUp");

            account.setIsRegistered(true);
            accountRepository.save(account);
            log.info("[{}] ✓ Registration complete", email);
        } else {
            log.info("[{}] Step 1: Already registered, skipping", email);
        }

        // ── Step 2: Authentication ──
        log.info("[{}] Step 2: Logging in...", email);
        performLogin(account);
        log.info("[{}] ✓ Login complete, token acquired", email);

        // ── Step 3: PDPA Consent (conditional) ──
        if (!Boolean.TRUE.equals(account.getIsPdpaAccepted())) {
            log.info("[{}] Step 3: Accepting PDPA consent...", email);
            executeWithRetry(() -> {
                apiClient.acceptPdpa(
                        account.getUserToken(),
                        account.getProxyHost(),
                        account.getProxyPort()
                );
                return null;
            }, account, "PDPA");

            account.setIsPdpaAccepted(true);
            accountRepository.save(account);
            log.info("[{}] ✓ PDPA accepted", email);
        } else {
            log.info("[{}] Step 3: PDPA already accepted, skipping", email);
        }

        // ── Step 4: Queue Monitoring Loop ──
        log.info("[{}] Step 4: Monitoring queue for shop {}...",
                email, account.getTargetBoardToken());

        String eligibleLineId = monitorQueueUntilReady(account);
        log.info("[{}] ✓ Queue condition met! Eligible line: {}", email, eligibleLineId);

        // ── Step 5: Anti-Fraud Bypass ──
        log.info("[{}] Step 5: Passing anti-fraud check...", email);
        executeWithRetry(() -> {
            apiClient.passAntifraud(
                    account.getUserToken(),
                    account.getDeviceUdid(),
                    account.getLatitude(),
                    account.getLongitude(),
                    account.getProxyHost(),
                    account.getProxyPort()
            );
            return null;
        }, account, "Antifraud");
        log.info("[{}] ✓ Anti-fraud passed", email);

        // ── Step 6: Submit Queue ──
        log.info("[{}] Step 6: Submitting queue reservation...", email);
        SubmitQueueResponse submitResponse = executeWithRetry(() ->
                apiClient.submitQueue(
                        account.getUserToken(),
                        eligibleLineId,
                        account.getCustomerQty(),
                        account.getProxyHost(),
                        account.getProxyPort()
                ), account, "SubmitQueue"
        );

        // SUCCESS!
        account.setQueueId(submitResponse.getQueueId());
        account.setStatus(AccountStatus.BOOKED);
        accountRepository.save(account);

        log.info("══════════════════════════════════════");
        log.info("🎉 [{}] BOOKED! Queue ID: {} | Queue No: {}",
                email, submitResponse.getQueueId(), submitResponse.getQueueNo());
        log.info("══════════════════════════════════════");
    }

    // ══════════════════════════════════════════════
    // QUEUE MONITORING
    // ══════════════════════════════════════════════

    /**
     * Polls the board list endpoint until a queue line meets the booking condition:
     * {@code current_queue > 0 AND current_queue <= queue_threshold}
     *
     * @param account The account to monitor for
     * @return The {@code line_id} of the first eligible queue line
     */
    private String monitorQueueUntilReady(AccountEntity account) throws Exception {
        String email = account.getEmail();
        int threshold = account.getQueueThreshold();

        while (true) {
            BoardListResponse boardList = executeWithRetry(() ->
                    apiClient.checkBoardList(
                            account.getTargetBoardToken(),
                            account.getProxyHost(),
                            account.getProxyPort()
                    ), account, "BoardList"
            );

            if (boardList.getLineList() != null) {
                for (BoardListResponse.QueueLine line : boardList.getLineList()) {
                    int currentQueue = line.getCurrentQueue() != null ? line.getCurrentQueue() : 0;
                    boolean isOpen = line.getIsOpen() != null && line.getIsOpen();

                    log.info("[{}] Queue '{}' → current: {} | threshold: {} | open: {}",
                            email, line.getLineName(), currentQueue, threshold, isOpen);

                    // Condition: queue > 0 (avoid no-show) AND queue <= threshold AND line is open
                    if (isOpen && currentQueue > 0 && currentQueue <= threshold) {
                        return line.getLineId();
                    }
                }
            }

            log.info("[{}] ⏳ Queue condition not met. Polling again in {}s...",
                    email, pollIntervalSeconds);
            Thread.sleep(pollIntervalSeconds * 1000L);
        }
    }

    // ══════════════════════════════════════════════
    // LOGIN HELPER
    // ══════════════════════════════════════════════

    /**
     * Performs login and stores the user_token in the account entity.
     */
    private void performLogin(AccountEntity account) throws Exception {
        LoginResponse loginResponse = executeWithRetry(() ->
                apiClient.login(
                        account.getEmail(),
                        account.getPassword(),
                        account.getProxyHost(),
                        account.getProxyPort()
                ), account, "Login"
        );

        account.setUserToken(loginResponse.getUserToken());
        accountRepository.save(account);
    }

    // ══════════════════════════════════════════════
    // RETRY LOGIC
    // ══════════════════════════════════════════════

    /**
     * Executes an API call with differentiated error handling:
     * <ul>
     *   <li>HTTP 5xx → Retry up to {@code maxRetryAttempts} with exponential backoff</li>
     *   <li>HTTP 401 → Re-login to refresh token, then retry once</li>
     *   <li>HTTP 403 → KILL SWITCH: mark account as ERROR, throw immediately</li>
     * </ul>
     *
     * @param action      The API call to execute (as a lambda)
     * @param account     The account context (for re-login and error marking)
     * @param actionName  Human-readable name for logging
     * @param <T>         Return type of the action
     * @return The result of the action
     */
    private <T> T executeWithRetry(RetryableAction<T> action,
                                    AccountEntity account,
                                    String actionName) throws Exception {
        int[] backoffSeconds = {5, 10, 15};

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                return action.execute();

            } catch (HttpResponseException e) {
                int httpStatus = e.getStatusCode();

                // ── 403 Forbidden: KILL SWITCH ──
                if (httpStatus == 403) {
                    log.error("🚨 [{}] HTTP 403 on {} → KILL SWITCH! Account likely banned.",
                            account.getEmail(), actionName);
                    markError(account, "HTTP 403 Forbidden (banned) during " + actionName);
                    throw e; // Propagate to stop processing this account
                }

                // ── 401 Unauthorized: Re-login ──
                if (httpStatus == 401) {
                    log.warn("🔑 [{}] HTTP 401 on {} → Re-authenticating...",
                            account.getEmail(), actionName);
                    try {
                        performLogin(account);
                        log.info("[{}] Re-login successful, retrying {}...",
                                account.getEmail(), actionName);
                        return action.execute(); // Retry once after re-login
                    } catch (Exception loginEx) {
                        log.error("[{}] Re-login failed: {}", account.getEmail(), loginEx.getMessage());
                        markError(account, "Re-login failed during " + actionName);
                        throw loginEx;
                    }
                }

                // ── 5xx Server Error: Retry with backoff ──
                if (httpStatus >= 500) {
                    int delay = backoffSeconds[Math.min(attempt - 1, backoffSeconds.length - 1)];
                    log.warn("⚡ [{}] HTTP {} on {} → Retry {}/{} in {}s",
                            account.getEmail(), httpStatus, actionName,
                            attempt, maxRetryAttempts, delay);

                    if (attempt == maxRetryAttempts) {
                        log.error("[{}] Max retries ({}) exhausted for {}",
                                account.getEmail(), maxRetryAttempts, actionName);
                        markError(account, "Max retries exhausted for " + actionName + " (HTTP " + httpStatus + ")");
                        throw e;
                    }

                    Thread.sleep(delay * 1000L);
                    continue; // Next attempt
                }

                // ── Other HTTP errors ──
                log.error("[{}] HTTP {} on {} → Unhandled status",
                        account.getEmail(), httpStatus, actionName);
                markError(account, "HTTP " + httpStatus + " during " + actionName);
                throw e;

            } catch (Exception e) {
                if (e instanceof HttpResponseException) throw e; // Already handled above

                // Network errors, JSON parse errors, etc.
                if (attempt == maxRetryAttempts) {
                    log.error("[{}] Non-HTTP error on {} (attempt {}/{}): {}",
                            account.getEmail(), actionName, attempt, maxRetryAttempts, e.getMessage());
                    markError(account, e.getMessage());
                    throw e;
                }

                int delay = backoffSeconds[Math.min(attempt - 1, backoffSeconds.length - 1)];
                log.warn("[{}] Error on {} → Retry {}/{} in {}s: {}",
                        account.getEmail(), actionName, attempt, maxRetryAttempts, delay, e.getMessage());
                Thread.sleep(delay * 1000L);
            }
        }

        // Should never reach here
        throw new RuntimeException("Unexpected: retry loop completed without returning or throwing");
    }

    // ══════════════════════════════════════════════
    // ERROR HANDLER
    // ══════════════════════════════════════════════

    /**
     * Marks an account as ERROR and persists the state.
     */
    private void markError(AccountEntity account, String reason) {
        account.setStatus(AccountStatus.ERROR);
        accountRepository.save(account);
        log.error("❌ [{}] Marked as ERROR: {}", account.getEmail(), reason);
    }

    // ══════════════════════════════════════════════
    // FUNCTIONAL INTERFACE
    // ══════════════════════════════════════════════

    /**
     * Functional interface for a retryable action that may throw exceptions.
     */
    @FunctionalInterface
    private interface RetryableAction<T> {
        T execute() throws Exception;
    }
}
