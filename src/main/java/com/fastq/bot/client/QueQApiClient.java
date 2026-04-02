package com.fastq.bot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastq.bot.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP client for all QueQ API interactions.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Build and send HTTP requests to QueQ endpoints</li>
 *   <li>Inject the required {@code User-Agent} header on every request</li>
 *   <li>Apply random jitter (configurable delay) before each request to evade anti-bot detection</li>
 *   <li>Support per-account proxy overrides</li>
 *   <li>Parse JSON responses into typed DTOs</li>
 * </ul>
 *
 * <h3>Host Mapping</h3>
 * <table>
 *   <tr><td>api1.queq.me</td><td>signup, login, boardList, antifraud, submitQueue, cancelQueue</td></tr>
 *   <tr><td>api0-portal.queq.me</td><td>UpdatePDPA</td></tr>
 * </table>
 */
@Slf4j
@Component
public class QueQApiClient {

    private static final String HOST_API1 = "https://api1.queq.me";
    private static final String HOST_PORTAL = "https://api0-portal.queq.me";

    private final HttpClient defaultHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${bot.user-agent}")
    private String userAgent;

    @Value("${bot.jitter.min-seconds}")
    private int jitterMinSeconds;

    @Value("${bot.jitter.max-seconds}")
    private int jitterMaxSeconds;

    public QueQApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.defaultHttpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // ──────────────────────────────────────────────
    // 1. SIGNUP
    // ──────────────────────────────────────────────

    /**
     * Registers a new QueQ account.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/QueQ/Customer_v3/signup.ashx}
     *
     * @param email    Account email
     * @param name     Display name
     * @param password Account password
     * @param proxyHost Optional per-account proxy host (nullable)
     * @param proxyPort Optional per-account proxy port (nullable)
     * @return Raw JSON response as a Map
     * @throws Exception on HTTP or parsing errors
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> signUp(String email, String name, String password,
                                       String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("signUp");

        Map<String, Object> payload = Map.of(
                "email", email,
                "name", name,
                "password", password,
                "password_confirm", password
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/QueQ/Customer_v3/signup.ashx",
                payload,
                null // no auth token for signup
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[SignUp] email={} | status={} | body={}", email, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), Map.class);
    }

    // ──────────────────────────────────────────────
    // 2. LOGIN
    // ──────────────────────────────────────────────

    /**
     * Authenticates and retrieves a {@code user_token}.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/loginEmail.ashx}
     *
     * @param email    Account email
     * @param password Account password
     * @param proxyHost Optional per-account proxy host
     * @param proxyPort Optional per-account proxy port
     * @return {@link LoginResponse} containing the user_token
     * @throws Exception on HTTP or parsing errors
     */
    public LoginResponse login(String email, String password,
                                String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("login");

        Map<String, Object> payload = Map.of(
                "email", email,
                "password", password
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/loginEmail.ashx",
                payload,
                null
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[Login] email={} | status={} | body={}", email, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), LoginResponse.class);
    }

    // ──────────────────────────────────────────────
    // 3. ACCEPT PDPA
    // ──────────────────────────────────────────────

    /**
     * Accepts PDPA (Personal Data Protection Act) consent.
     * <p>
     * Endpoint: {@code POST https://api0-portal.queq.me/QueqPortal/Customer/UpdatePDPA}
     * <br>
     * Requires special header: {@code X-QueqPortal-UserToken}
     *
     * @param userToken The user_token from login
     * @param proxyHost Optional proxy host
     * @param proxyPort Optional proxy port
     * @return Raw response as Map
     * @throws Exception on HTTP or parsing errors
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> acceptPdpa(String userToken,
                                           String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("acceptPdpa");

        String jsonBody = objectMapper.writeValueAsString(Map.of("status", 1));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HOST_PORTAL + "/QueqPortal/Customer/UpdatePDPA"))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json")
                .header("X-QueqPortal-UserToken", userToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[PDPA] token={}... | status={} | body={}",
                userToken.substring(0, Math.min(8, userToken.length())),
                response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), Map.class);
    }

    // ──────────────────────────────────────────────
    // 4. CHECK BOARD LIST (Queue Monitoring)
    // ──────────────────────────────────────────────

    /**
     * Retrieves the current queue status for a target shop.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/reqBoardList.ashx}
     *
     * @param boardToken Target shop board token
     * @param proxyHost  Optional proxy host
     * @param proxyPort  Optional proxy port
     * @return {@link BoardListResponse} with queue lines and counts
     * @throws Exception on HTTP or parsing errors
     */
    public BoardListResponse checkBoardList(String boardToken,
                                             String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("checkBoardList");

        Map<String, Object> payload = Map.of(
                "board_token", boardToken
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/reqBoardList.ashx",
                payload,
                null
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("[BoardList] board={} | status={} | body={}", boardToken, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), BoardListResponse.class);
    }

    // ──────────────────────────────────────────────
    // 5. ANTI-FRAUD BYPASS
    // ──────────────────────────────────────────────

    /**
     * Submits device integrity verification to pass the anti-fraud check.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/Queue/Antifraud}
     *
     * @param userToken The user_token from login
     * @param udid      Device UDID (UUID v4 upper case, locked to account)
     * @param lat       GPS latitude near the target shop
     * @param lon       GPS longitude near the target shop
     * @param proxyHost Optional proxy host
     * @param proxyPort Optional proxy port
     * @return {@link AntifraudResponse}
     * @throws Exception on HTTP or parsing errors
     */
    public AntifraudResponse passAntifraud(String userToken, String udid,
                                            double lat, double lon,
                                            String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("passAntifraud");

        Map<String, Object> payload = Map.of(
                "user_token", userToken,
                "UDID", udid,
                "lat", String.valueOf(lat),
                "lon", String.valueOf(lon)
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/Queue/Antifraud",
                payload,
                userToken
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[Antifraud] udid={} | status={} | body={}", udid, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), AntifraudResponse.class);
    }

    // ──────────────────────────────────────────────
    // 6. SUBMIT QUEUE
    // ──────────────────────────────────────────────

    /**
     * Submits a queue reservation for the target shop.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/submitQueue.ashx}
     *
     * @param userToken   The user_token from login
     * @param queueLineId The target queue line ID from boardList
     * @param customerQty Number of customers (party size)
     * @param proxyHost   Optional proxy host
     * @param proxyPort   Optional proxy port
     * @return {@link SubmitQueueResponse} with queue_id on success
     * @throws Exception on HTTP or parsing errors
     */
    public SubmitQueueResponse submitQueue(String userToken, String queueLineId,
                                            int customerQty,
                                            String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("submitQueue");

        Map<String, Object> payload = Map.of(
                "user_token", userToken,
                "queue_line_id", queueLineId,
                "customer_qty", customerQty
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/submitQueue.ashx",
                payload,
                userToken
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[SubmitQueue] lineId={} | status={} | body={}", queueLineId, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), SubmitQueueResponse.class);
    }

    // ──────────────────────────────────────────────
    // 7. CANCEL QUEUE (Fallback)
    // ──────────────────────────────────────────────

    /**
     * Cancels an existing queue reservation.
     * <p>
     * Endpoint: {@code POST https://api1.queq.me/cancelQueue.ashx}
     *
     * @param userToken The user_token from login
     * @param queueId   The queue_id to cancel
     * @param proxyHost Optional proxy host
     * @param proxyPort Optional proxy port
     * @return Raw response as Map
     * @throws Exception on HTTP or parsing errors
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelQueue(String userToken, String queueId,
                                            String proxyHost, Integer proxyPort) throws Exception {
        applyJitter("cancelQueue");

        Map<String, Object> payload = Map.of(
                "user_token", userToken,
                "queue_id", queueId
        );

        HttpRequest request = buildJsonPostRequest(
                HOST_API1 + "/cancelQueue.ashx",
                payload,
                userToken
        );

        HttpClient client = resolveClient(proxyHost, proxyPort);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("[CancelQueue] queueId={} | status={} | body={}", queueId, response.statusCode(), response.body());
        checkResponseStatus(response);

        return objectMapper.readValue(response.body(), Map.class);
    }

    // ══════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════

    /**
     * Builds a JSON POST request with the common User-Agent header.
     *
     * @param url       Full URL
     * @param payload   Request body as a Map (will be serialized to JSON)
     * @param authToken Optional auth token (added as user_token in body if needed)
     * @return Configured HttpRequest
     */
    private HttpRequest buildJsonPostRequest(String url, Map<String, Object> payload,
                                              String authToken) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        return builder.build();
    }

    /**
     * Resolves which HttpClient to use:
     * <ul>
     *   <li>If per-account proxy is set → create a dedicated HttpClient with that proxy</li>
     *   <li>Otherwise → use the default (global) HttpClient bean</li>
     * </ul>
     */
    private HttpClient resolveClient(String proxyHost, Integer proxyPort) {
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort != null && proxyPort > 0) {
            log.debug("[Proxy] Using per-account proxy {}:{}", proxyHost, proxyPort);
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(30))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .executor(Executors.newVirtualThreadPerTaskExecutor())
                    .proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)))
                    .build();
        }
        return defaultHttpClient;
    }

    /**
     * Applies a random delay (jitter) before sending a request.
     * This simulates human-like behavior to evade anti-bot detection.
     *
     * @param methodName Name of the calling method (for logging)
     */
    private void applyJitter(String methodName) {
        int delaySeconds = ThreadLocalRandom.current().nextInt(jitterMinSeconds, jitterMaxSeconds + 1);
        log.debug("[Jitter] {} → waiting {}s before request", methodName, delaySeconds);
        try {
            Thread.sleep(Duration.ofSeconds(delaySeconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Jitter] Interrupted during delay for {}", methodName);
        }
    }

    /**
     * Returns the HTTP status code from a response.
     * Used by the service layer for retry/error handling logic.
     */
    public int getHttpStatus(HttpResponse<?> response) {
        return response.statusCode();
    }

    /**
     * Throws an {@link HttpResponseException} if the HTTP status indicates a
     * server error (5xx), unauthorized (401), or forbidden (403).
     * <p>
     * The service layer uses the exception's status code to decide whether
     * to retry, re-login, or kill-switch the account.
     */
    private void checkResponseStatus(HttpResponse<String> response) throws HttpResponseException {
        int status = response.statusCode();
        if (status == 401 || status == 403 || status >= 500) {
            throw new HttpResponseException(status, response.body());
        }
    }

    // ──────────────────────────────────────────────
    // Custom Exception
    // ──────────────────────────────────────────────

    /**
     * Exception that carries the HTTP status code for the service layer
     * to implement differentiated error handling (retry vs re-login vs kill-switch).
     */
    public static class HttpResponseException extends Exception {
        private final int statusCode;
        private final String responseBody;

        public HttpResponseException(int statusCode, String responseBody) {
            super("HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
