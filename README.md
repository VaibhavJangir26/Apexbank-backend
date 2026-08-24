# ApexBank: High-Concurrency Core Banking Engine

ApexBank is a production-grade, high-performance core banking backend engine built using **Spring Boot (Java 17)**, **PostgreSQL**, **Redis**, and **Redisson**. It is engineered to satisfy the highest industry standards of transactional integrity, reliability, and security under high concurrent loads.

The architecture is specifically designed to guarantee **ACID properties**, eliminate **race conditions/deadlocks**, enforce **idempotency**, and maximize performance using advanced caching strategies.

---

## 🏗️ Architectural Core Pillars (Production Engineering)

### 1. High-Concurrency Financial Transfer Engine
The system uses a multi-layered transaction processing engine designed to defend against double-spending, race conditions, database connection pool exhaustion, and network instability:

```
[Transfer Request]
       │
       ▼
 ┌───────────┐      Exists?
 │Idempotency│ ───► [Throw ConflictException (409)]
 │  Engine   │
 └─────┬─────┘
       │ Redis SETNX (5m TTL)
       ▼
 ┌───────────┐      Exceeds Max Transaction Limit?
 │ Pre-Lock  │ ───► [Throw IllegalArgumentException (400)]
 │  Check    │
 └─────┬─────┘
       │
       ▼
 ┌───────────┐      Ordered locking (locks smaller accountNo first)
 │  Lock     │ ───► Prevents circular wait (Dijkstra's Lock Ordering)
 │  Sorter   │
 └─────┬─────┘
       │
       ▼
 ┌───────────┐      Acquire Distributed MultiLock (Wait 3s, Hold 5s)
 │ Redisson  │ ───► Queue requests in Redis RAM instead of blocking DB connections
 │ MultiLock │
 └─────┬─────┘
       │
       ▼
 ┌───────────┐      INCR daily count key in Redis
 │  Atomic   │ ───► Evicts/rollbacks atomically if validation fails
 │  Counter  │
 └─────┬─────┘
       │
       ▼
 ┌───────────┐      Spring @Transactional + JPA @Version
 │Database DB│ ───► @Retryable for ObjectOptimisticLockingFailureException
 │Execution  │
 └─────┬─────┘
       │
       ▼
 ┌───────────┐      Set Idempotency to SUCCESS (24h TTL)
 │  Final    │ ───► Evict account cache and transaction history
 │  Cleanup  │
 └───────────┘
```

#### 🛡️ Concurrency Controls & Deadlock Prevention
* **Deterministic Lock Sorting (Dijkstra's Algorithm)**: To prevent circular deadlock conditions (where User A transfers to User B while User B transfers to User A simultaneously), lock keys are sorted lexicographically before acquisition.
  ```java
  String firstLockKey = accountFrom.compareTo(accountTo) < 0 ? accountFrom : accountTo;
  String secondLockKey = accountFrom.compareTo(accountTo) < 0 ? accountTo : accountFrom;
  ```
* **Redisson MultiLock**: Locks both accounts atomically in a single operation. This ensures that no thread can block indefinitely holding one lock while waiting for another, maintaining a strictly lock-ordered hierarchy.
* **Lease-Time Protection**: Distributed locks are acquired with an automatic lease-time (e.g. 5 seconds) to ensure that if a server node crashes during execution, the locks are automatically released and do not orphan.

#### 🔄 Transaction Integrity & Optimistic Locking
* **Spring `@Transactional(rollbackFor = Exception.class)`**: Enforces database ACID properties, automatically rolling back PostgreSQL state changes on runtime exceptions.
* **JPA Optimistic Locking (`@Version`)**: The `Account` entity incorporates a `@Version` field to prevent lost updates under heavy write surges.
* **Exponential Backoff Retries**: If optimistic lock contention occurs, the database execution block is automatically retried up to 3 times:
  ```java
  @Retryable(
      retryFor = { ObjectOptimisticLockingFailureException.class },
      maxAttempts = 3,
      backoff = @Backoff(delay = 50, multiplier = 2)
  )
  ```

#### ⚡ High-Speed Rate Limiting (Atomic Count Enforcement)
* **Atomic Redis Counters**: Rather than locking the database table or reading previous transaction histories to check daily counts, the engine utilizes Redis atomic increments:
  ```java
  Long currentDailyCount = redisTemplate.opsForValue().increment(dailyCountKey);
  ```
  This isolates counting operations in Redis RAM, shielding PostgreSQL from heavy read traffic.

### 2. Header-Based Idempotency Engine
To prevent double-billing and duplicate form submissions caused by network retries or client errors:
* **Unique Transaction Key Constraint**: Uses a Redis-based distributed token store. Every write request must pass a unique transaction reference (validated against the pattern `^TNX-APEXBANK-[a-zA-Z0-9-]+$`).
* **Stateful Lifecycle Processing**:
  - Checks if the token is present in Redis via `SETNX`.
  - If absent, puts it in `PROCESSING` state (5-minute TTL).
  - If present and in `PROCESSING`, throws `409 Conflict` (prevents double processing).
  - Upon successful transaction commit, transitions the state to `SUCCESS` (24-hour retention TTL).

### 3. Distributed Cache Architecture
To maximize throughput and deliver sub-millisecond API response times:
* **Dynamic Cache Eviction**: Integrates Spring Cache backed by Redis. Details and ledgers are cached on read (`@Cacheable`), but strictly evicted (`@CacheEvict`) upon transactions, status modifications, or limit adjustments to prevent dirty reads.
* **Redis TTL Policies**: System utilizes strict TTL policies (10-minute expirations for core account lookups, 7-day expirations for security refresh token claims).

---

## 🛠️ Complete Backend API Catalog

### 1. Authentication & Session Services (`/api/v1/auth`)
* `POST /signup` - Caches registration details in Redis, generates and triggers a 6-digit verification code.
* `POST /verify` - Compares OTP, saves user entity to DB, generates JWT access token and refresh token, and maps session UUID to Redis.
* `POST /login` - Standard credentials evaluation and active session initialization.
* `POST /refresh` - Validates the refresh token against active Redis sessions and provides a new access token.
* `POST /logout` - Revokes refresh token in Redis and invalidates security context.

### 2. User Profiles (`/api/v1/profile`)
* `GET /me` - Fetches profile details (Username, Email, verified KYC data).
* `PATCH /me` - Updates name, contact number, and address.

### 3. Bank Account Management (`/api/v1/account`)
* `POST /` - Registers a new bank account (Customer only).
* `GET /` - Retrieves all accounts registered under the company (Admin/Manager only).
* `GET /me` - Fetches accounts registered under the currently logged-in customer.
* `GET /{accountNo}` - Detailed lookup of an account (Admin/Manager/Authorized Customer).
* `PATCH /status` - Updates status (`ACTIVE`, `INACTIVE`, `SUSPEND`, `DELETE`) with dynamic cache eviction.
* `POST /transfer` - Executes transfer with multi-lock constraints.
* `GET /{accountNo}/transactions` - Fetches the double-entry transaction history ledger.

### 4. Policy limits & Ticketing (`/api/v1/transaction-limit`)
* `POST /` - Updates velocity limits directly (Admin/Manager only).
* `GET /{accountNo}` - Retrieves limits (resolves from Redis cache -> falls back to database -> system default).
* `POST /ticket` - Raises a request to elevate daily count & transfer amount (Customer only).
* `GET /tickets` - Fetches all upgrade tickets (Admin/Manager only).
* `GET /tickets/account/{accountNo}` - Fetches ticket history list for a specific account (Customer/Admin/Manager).
* `PATCH /ticket/{ticketId}/resolve` - Resolves (approves and applies limits / rejects with comment) a ticket (Admin/Manager only).

---

## ⚙️ Development Configuration

Create a `.env` file in the root directory `apexbank/apexbank/` with the following variables:

```env
# Database Settings
DB_URL=jdbc:postgresql://localhost:5432/apexbank
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Config
JWT_SECRET=your_super_secret_signing_key_at_least_256_bits_long
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Mail Server Config
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

---

## 🏃 Build & Launch Guidelines

### Compilation & Static Checks
```bash
./mvnw clean compile
```

### Run Application Local
```bash
./mvnw spring-boot:run
```

### Packaging Production Jar
```bash
./mvnw clean package -DskipTests
```
Run packaged executable:
```bash
java -jar target/apexbank-0.0.1-SNAPSHOT.jar
```
The application bootstraps on **Port 8600** by default.
