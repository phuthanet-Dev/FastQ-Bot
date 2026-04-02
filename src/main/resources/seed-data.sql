-- ══════════════════════════════════════════════════════════
-- FastQ-Bot: Database Setup & Sample Data
-- ══════════════════════════════════════════════════════════
-- Run this AFTER the application has started once (so Hibernate
-- creates the 'accounts' table via ddl-auto: update).
--
-- Usage:
--   psql -U postgres -d fastqbot -f src/main/resources/seed-data.sql
-- ══════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────
-- Example: Insert a test account targeting a specific shop
-- ──────────────────────────────────────────────
INSERT INTO accounts (
    email,
    password,
    account_name,
    target_board_token,
    device_udid,
    is_registered,
    is_pdpa_accepted,
    status,
    latitude,
    longitude,
    queue_threshold,
    customer_qty
) VALUES (
    'test.user01@gmail.com',           -- email
    'SecurePass123!',                   -- password
    'John Doe',                         -- account_name (display name)
    'SHOP_BOARD_TOKEN_HERE',            -- target_board_token (replace with actual shop ID)
    UPPER(gen_random_uuid()::text),     -- device_udid (UUID v4 uppercase, generated once)
    false,                              -- is_registered (bot will register first)
    false,                              -- is_pdpa_accepted (bot will accept PDPA)
    'IDLE',                             -- status
    13.7563,                            -- latitude (Bangkok example)
    100.5018,                           -- longitude (Bangkok example)
    20,                                 -- queue_threshold (book when queue <= 20)
    2                                   -- customer_qty (party of 2)
);

-- ──────────────────────────────────────────────
-- Example: Insert a pre-registered account (skip signup)
-- ──────────────────────────────────────────────
-- INSERT INTO accounts (
--     email, password, account_name, target_board_token,
--     device_udid, is_registered, is_pdpa_accepted, status,
--     latitude, longitude, queue_threshold, customer_qty
-- ) VALUES (
--     'existing.user@gmail.com', 'MyPassword456!', 'Jane Smith',
--     'ANOTHER_SHOP_TOKEN',
--     UPPER(gen_random_uuid()::text),
--     true,   -- already registered
--     true,   -- already accepted PDPA
--     'IDLE',
--     13.7450, 100.5340,
--     15, 1
-- );

-- ──────────────────────────────────────────────
-- Useful queries for monitoring
-- ──────────────────────────────────────────────

-- View all accounts and their status:
-- SELECT id, email, status, queue_id, is_registered, is_pdpa_accepted FROM accounts;

-- Find successfully booked accounts:
-- SELECT email, queue_id, account_name FROM accounts WHERE status = 'BOOKED';

-- Reset all accounts to IDLE (for re-testing):
-- UPDATE accounts SET status = 'IDLE', queue_id = NULL, user_token = NULL;
