package com.fastq.bot.enums;

/**
 * Represents the lifecycle status of an account in the booking workflow.
 *
 * <ul>
 *   <li><b>IDLE</b> – Account is registered but not yet processing.</li>
 *   <li><b>WAITING</b> – Account is actively monitoring the queue, waiting for the right condition to book.</li>
 *   <li><b>BOOKED</b> – Account has successfully submitted a queue reservation.</li>
 *   <li><b>ERROR</b> – An unrecoverable error occurred (e.g., HTTP 403 ban). The account is halted.</li>
 * </ul>
 */
public enum AccountStatus {
    IDLE,
    WAITING,
    BOOKED,
    ERROR
}
