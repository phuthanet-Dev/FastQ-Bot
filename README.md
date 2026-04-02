# 🚀 FastQ-Bot

**Automated Queue Booking System** — Built with Java 21 Virtual Threads & Spring Boot

An automation tool that orchestrates reverse-engineered QueQ API calls to book restaurant queues across multiple accounts concurrently. Each account runs in its own lightweight Virtual Thread, targeting a single shop with anti-detection measures baked in.

---

## 🏗️ Architecture

```
┌─────────────────────────┐
│   BotCommandLineRunner  │  ← Auto-starts on boot
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ QueueAutomationService  │  ← Orchestration engine
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│  Virtual Thread Pool    │  ← 1 thread per account
│  ┌───┐ ┌───┐ ┌───┐     │
│  │ A │ │ B │ │ N │     │
│  └─┬─┘ └─┬─┘ └─┬─┘     │
└────┼─────┼─────┼────────┘
     └─────┼─────┘
           ▼
┌─────────────────────────┐
│     QueQApiClient       │  ← HTTP client (java.net.http)
└───────────┬─────────────┘
            ▼
    ┌───────┴───────┐
    ▼               ▼
api1.queq.me   api0-portal.queq.me
```

## ✨ Features

| Feature | Description |
|---|---|
| **Java 21 Virtual Threads** | Lightweight concurrency — process hundreds of accounts without thread pool limits |
| **1 Account = 1 Shop** | Isolation strategy to minimize mass-ban risk |
| **Random Jitter** | 5-30s random delay between API requests to simulate human behavior |
| **Smart Queue Trigger** | Books only when `current_queue > 0 AND <= threshold` — avoids no-show bans |
| **Differentiated Retry** | 5xx → Backoff retry, 401 → Re-login, 403 → Kill switch |
| **Per-Account Proxy** | Optional proxy per account for IP isolation |
| **Locked UDID** | UUID v4 generated once, permanently tied to each account |

## 📋 Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.4.4
- **HTTP Client:** `java.net.http.HttpClient` (native)
- **Database:** PostgreSQL + Spring Data JPA
- **Build:** Maven

## 🔄 Workflow

```
For each account:
  1. 📝 Register    (skip if already registered)
  2. 🔐 Login       → get user_token
  3. 📄 Accept PDPA (skip if already accepted)
  4. 👀 Monitor     → poll queue until condition met
  5. 🛡️ Anti-Fraud  → device integrity check (UDID + GPS)
  6. 🎯 Submit      → book the queue!
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** — `java -version`
- **Maven** — `mvn -version`
- **PostgreSQL** — running locally

### 1. Clone

```bash
git clone https://github.com/phuthanet-Dev/FastQ-Bot.git
cd FastQ-Bot
```

### 2. Create Database

```bash
psql -U postgres -c "CREATE DATABASE fastqbot;"
```

### 3. Configure

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fastqbot
    username: postgres
    password: YOUR_PASSWORD_HERE
```

### 4. Run

```bash
mvn spring-boot:run
```

The app will auto-create the `accounts` table via Hibernate.

### 5. Seed Account Data

```bash
psql -U postgres -d fastqbot -f src/main/resources/seed-data.sql
```

Or insert manually:

```sql
INSERT INTO accounts (
    email, password, account_name, target_board_token,
    device_udid, is_registered, is_pdpa_accepted, status,
    latitude, longitude, queue_threshold, customer_qty
) VALUES (
    'your.email@gmail.com', 'YourPassword123!', 'Your Name',
    'TARGET_SHOP_BOARD_TOKEN',
    UPPER(gen_random_uuid()::text),
    false, false, 'IDLE',
    13.7563, 100.5018,   -- GPS near the shop
    20, 2                -- threshold, party size
);
```

### 6. Run Again

```bash
mvn spring-boot:run
```

The bot picks up all `IDLE` accounts and starts the workflow automatically.

## 📁 Project Structure

```
src/main/java/com/fastq/bot/
├── FastQBotApplication.java          # Entry point
├── config/
│   └── HttpClientConfig.java        # HttpClient + Virtual Thread executor
├── entity/
│   └── AccountEntity.java           # JPA Entity (accounts table)
├── enums/
│   └── AccountStatus.java           # IDLE, WAITING, BOOKED, ERROR
├── repository/
│   └── AccountRepository.java       # Spring Data JPA
├── dto/
│   ├── LoginResponse.java           # Login API response
│   ├── BoardListResponse.java       # Queue monitoring response
│   ├── AntifraudResponse.java       # Anti-fraud response
│   └── SubmitQueueResponse.java     # Queue submission response
├── client/
│   └── QueQApiClient.java           # All 7 API methods
├── service/
│   └── QueueAutomationService.java  # Workflow orchestration
└── runner/
    └── BotCommandLineRunner.java    # Auto-start on boot
```

## ⚙️ Configuration

All settings in `application.yml`:

```yaml
bot:
  user-agent: "QueQ/4.16.46 (iPhone; iOS 26.3; Scale/3.00)"
  jitter:
    min-seconds: 5       # Min random delay
    max-seconds: 30      # Max random delay
  proxy:
    enabled: false       # Global proxy toggle
    host: ""
    port: 0
  retry:
    max-attempts: 3      # Max retries on 5xx
    backoff-seconds: [5, 10, 15]
  poll-interval-seconds: 10  # Queue check interval
```

## 🛡️ Error Handling

| HTTP Status | Action |
|---|---|
| **5xx** | Retry up to 3× with backoff (5s, 10s, 15s) |
| **401** | Re-login to refresh `user_token`, then retry |
| **403** | 🚨 **KILL SWITCH** — mark account as `ERROR`, stop immediately |

## 📊 Monitoring

```sql
-- View all accounts
SELECT id, email, status, queue_id FROM accounts;

-- Booked accounts
SELECT email, queue_id, account_name FROM accounts WHERE status = 'BOOKED';

-- Reset for re-testing
UPDATE accounts SET status = 'IDLE', queue_id = NULL, user_token = NULL;
```

## 📄 License

This project is for **educational and research purposes** only.
