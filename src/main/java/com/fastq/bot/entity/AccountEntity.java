package com.fastq.bot.entity;

import com.fastq.bot.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * JPA Entity representing a QueQ account and its booking metadata.
 * <p>
 * Each account is isolated to a single target shop ("1 Account per 1 Target Shop" strategy)
 * to minimize the risk of mass bans. The device UDID is generated once and permanently
 * tied to the email address to pass anti-bot device integrity checks.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "userToken"})
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * QueQ account email address. Must be unique across the system.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * QueQ account password. Stored in plaintext because booked accounts
     * need to re-authenticate (login) on every run.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Display name used during signup and shown on the queue ticket.
     */
    @Column(name = "account_name", nullable = false)
    private String accountName;

    /**
     * The target shop/board token (ID). Each account targets exactly one shop.
     */
    @Column(name = "target_board_token", nullable = false)
    private String targetBoardToken;

    /**
     * Device UDID — UUID v4 in UPPER CASE. Generated once during entity creation
     * via {@link #prePersist()} and permanently tied to this email.
     * <p>
     * NEVER regenerate this value after initial creation.
     */
    @Column(name = "device_udid", nullable = false, updatable = false)
    private String deviceUdid;

    /**
     * Whether the signup API has been called for this account.
     */
    @Column(name = "is_registered", nullable = false)
    @Builder.Default
    private Boolean isRegistered = false;

    /**
     * Whether the PDPA consent API has been called for this account.
     */
    @Column(name = "is_pdpa_accepted", nullable = false)
    @Builder.Default
    private Boolean isPdpaAccepted = false;

    /**
     * Current lifecycle status of this account in the booking workflow.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.IDLE;

    /**
     * Cached user token obtained from the login API.
     * Refreshed when a 401 Unauthorized is received.
     */
    @Column(name = "user_token")
    private String userToken;

    /**
     * GPS latitude for the anti-fraud device integrity check.
     * Should be set to a coordinate near the target shop.
     */
    @Column
    private Double latitude;

    /**
     * GPS longitude for the anti-fraud device integrity check.
     * Should be set to a coordinate near the target shop.
     */
    @Column
    private Double longitude;

    /**
     * Queue ID returned after a successful booking (submitQueue).
     * Used for cancellation if needed.
     */
    @Column(name = "queue_id")
    private String queueId;

    /**
     * Maximum queue length threshold. The bot will only book when:
     * {@code current_queue > 0 AND current_queue <= queue_threshold}
     * <p>
     * This prevents "no-show" bans from booking an empty queue.
     */
    @Column(name = "queue_threshold")
    @Builder.Default
    private Integer queueThreshold = 20;

    /**
     * Number of customers (party size) to submit with the queue booking.
     */
    @Column(name = "customer_qty")
    @Builder.Default
    private Integer customerQty = 1;

    /**
     * Optional per-account HTTP proxy host.
     * If null, the global proxy from application.yml is used.
     */
    @Column(name = "proxy_host")
    private String proxyHost;

    /**
     * Optional per-account HTTP proxy port.
     */
    @Column(name = "proxy_port")
    private Integer proxyPort;

    /**
     * Automatically generates a UUID v4 (UPPER CASE) device UDID
     * before the entity is first persisted. This ensures the UDID
     * is created once and never changes.
     */
    @PrePersist
    protected void prePersist() {
        if (this.deviceUdid == null || this.deviceUdid.isBlank()) {
            this.deviceUdid = UUID.randomUUID().toString().toUpperCase();
        }
    }
}
