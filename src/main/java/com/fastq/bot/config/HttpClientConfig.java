package com.fastq.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configures the {@link java.net.http.HttpClient} bean used by {@code QueQApiClient}.
 * <p>
 * Features:
 * <ul>
 *   <li>Uses a Virtual Thread executor for non-blocking I/O</li>
 *   <li>Global proxy support (configurable via {@code bot.proxy.*} in application.yml)</li>
 *   <li>30-second connection timeout</li>
 *   <li>Automatic redirect following</li>
 * </ul>
 */
@Configuration
public class HttpClientConfig {

    @Value("${bot.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${bot.proxy.host:}")
    private String proxyHost;

    @Value("${bot.proxy.port:0}")
    private int proxyPort;

    /**
     * Creates the primary {@link HttpClient} bean.
     * If proxy is enabled in config, all requests will route through it.
     * Per-account proxy overrides are handled in {@code QueQApiClient} by
     * creating a dedicated HttpClient instance for those accounts.
     */
    @Bean
    public HttpClient httpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newVirtualThreadPerTaskExecutor());

        if (proxyEnabled && proxyHost != null && !proxyHost.isBlank()) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }

        return builder.build();
    }

    /**
     * Virtual Thread executor bean for concurrent multi-account processing.
     * Each account runs in its own lightweight virtual thread.
     */
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
