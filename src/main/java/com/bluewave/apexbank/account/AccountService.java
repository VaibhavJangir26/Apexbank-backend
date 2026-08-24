package com.bluewave.apexbank.account;
//
//import com.bluewave.apexbank.account.dto.*;
//import com.bluewave.apexbank.config.RedisCacheConfig;
//import com.bluewave.apexbank.profiles.ProfileRepo;
//import com.bluewave.apexbank.profiles.Profiles;
//import com.bluewave.apexbank.users.Users;
//import com.bluewave.apexbank.utils.common.*;
//import com.bluewave.apexbank.utils.exceptions.ConflictException;
//import com.bluewave.apexbank.utils.exceptions.ForbiddenException;
//import com.bluewave.apexbank.utils.exceptions.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.orm.ObjectOptimisticLockingFailureException;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.security.SecureRandom;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AccountService {
//
//    private final AccountRepo accountRepo;
//    private final TransactionRepo transactionRepo;
//    private final ProfileRepo profileRepo;
//    private final SecurityUtils securityUtils;
//    private final StringRedisTemplate redisTemplate;
//    private final CacheManager cacheManager;
//    private final RedissonClient redissonClient;
//
//    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
//
//    private String generate12DigitAccountNo() {
//        SecureRandom random = new SecureRandom();
//        long number = 100000000000L + (long)(random.nextDouble() * 900000000000L);
//        return String.valueOf(number);
//    }
//
//    private boolean isAccountExistsAndValidate(String accountNo) {
//        if (accountNo == null || accountNo.length() != 12) {
//            return false;
//        }
//        return accountRepo.existsByAccountNo(accountNo);
//    }
//
//    @Transactional(rollbackFor = Exception.class)
//    public CommonApiResponse<String> createAccount(CreateAccountRequestDTO dto) {
//        log.info("[CREATE ACCOUNT] Creating new account for Profile ID: {}", dto.getProfileId());
//
//        String accountNo = generate12DigitAccountNo();
//
//        Profiles profile = profileRepo.findById(dto.getProfileId())
//                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + dto.getProfileId()));
//
//        Account account = new Account();
//        account.setAccountNo(accountNo);
//        account.setAccountType(dto.getAccountType());
//        account.setBalance(dto.getInitialAmount() != null ? dto.getInitialAmount() : BigDecimal.ZERO);
//        account.setUsersAccountStatus(UsersAccountStatus.ACTIVE);
//        account.setUsers(profile.getUsers());
//
//        accountRepo.save(account);
//
//        log.info("[CREATE ACCOUNT SUCCESS] Created Account No: {} for Profile ID: {}", accountNo, dto.getProfileId());
//
//        return CommonApiResponse.<String>builder()
//                .data("Account Number: " + accountNo)
//                .message("New account created successfully")
//                .statusCode(HttpStatus.CREATED.value())
//                .timestamp(LocalDateTime.now())
//                .success(true)
//                .build();
//    }
//
//    @Transactional(readOnly = true)
//    public CommonApiResponse<List<AccountResponseDTO>> getMyAccountDetails() {
//
//        Users currentUser = securityUtils.getCurrentUserEntity();
//        Set<String> roles = securityUtils.getUsersRoles(currentUser);
//
//        if (!roles.contains("ROLE_CUSTOMER")) {
//            throw new ForbiddenException("Role not permitted to access this resource");
//        }
//
//        List<Account> accounts = accountRepo.findAllByUsers_Username(currentUser.getUsername());
//
//        if (accounts.isEmpty()) {
//            throw new ResourceNotFoundException("No account details found for current user");
//        }
//
//        List<AccountResponseDTO> responseDTOs = accounts.stream()
//                .map(account -> AccountResponseDTO.builder()
//                        .accountNo(account.getAccountNo())
//                        .balance(account.getBalance())
//                        .accountType(account.getAccountType())
//                        .usersAccountStatus(account.getUsersAccountStatus())
//                        .username(currentUser.getUsername())
//                        .email(currentUser.getEmail())
//                        .createdAt(account.getCreatedAt())
//                        .build())
//                .toList();
//
//        return CommonApiResponse.<List<AccountResponseDTO>>builder()
//                .message("Account details fetched successfully")
//                .statusCode(HttpStatus.OK.value())
//                .data(responseDTOs)
//                .success(true)
//                .timestamp(LocalDateTime.now())
//                .build();
//    }
//
//    @Cacheable(value = RedisCacheConfig.CACHE_ACCOUNT_DETAILS, key = "#accountNo")
//    @Transactional(readOnly = true)
//    public CommonApiResponse<AccountResponseDTO> getAccountDetailsByAccountNo(String accountNo) {
//        log.info("[CACHE MISS] Fetching account details from DB for Account No: {}", accountNo);
//
//        if (!isAccountExistsAndValidate(accountNo)) {
//            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
//        }
//
//        Account account = accountRepo.findByAccountNo(accountNo)
//                .orElseThrow(() -> new ResourceNotFoundException("Account details not found"));
//
//        AccountResponseDTO responseDTO = AccountResponseDTO.builder()
//                .accountNo(account.getAccountNo())
//                .balance(account.getBalance())
//                .accountType(account.getAccountType())
//                .usersAccountStatus(account.getUsersAccountStatus())
//                .username(account.getUsers() != null ? account.getUsers().getUsername() : null)
//                .email(account.getUsers() != null ? account.getUsers().getEmail() : null)
//                .createdAt(account.getCreatedAt())
//                .build();
//
//        return CommonApiResponse.<AccountResponseDTO>builder()
//                .message("Account details fetched successfully")
//                .statusCode(HttpStatus.OK.value())
//                .data(responseDTO)
//                .success(true)
//                .timestamp(LocalDateTime.now())
//                .build();
//    }
//
//    @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNT_DETAILS, key = "#dto.accountNo")
//    @Transactional(rollbackFor = Exception.class)
//    public CommonApiResponse<AccountResponseDTO> updateUserAccountStatus(UpdateUserAccountStatusRequestDTO dto) {
//        if (!isAccountExistsAndValidate(dto.getAccountNo())) {
//            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
//        }
//
//        Account account = accountRepo.findByAccountNo(dto.getAccountNo())
//                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
//
//        account.setUsersAccountStatus(dto.getUsersAccountStatus());
//        accountRepo.save(account);
//
//        AccountResponseDTO responseDTO = AccountResponseDTO.builder()
//                .accountNo(account.getAccountNo())
//                .balance(account.getBalance())
//                .accountType(account.getAccountType())
//                .usersAccountStatus(account.getUsersAccountStatus())
//                .createdAt(account.getCreatedAt())
//                .build();
//
//        return CommonApiResponse.<AccountResponseDTO>builder()
//                .message("Account status updated successfully")
//                .statusCode(HttpStatus.OK.value())
//                .data(responseDTO)
//                .success(true)
//                .timestamp(LocalDateTime.now())
//                .build();
//    }
//
//    @Cacheable(value = RedisCacheConfig.CACHE_TRANSACTION_HISTORY, key = "#accountNo")
//    @Transactional(readOnly = true)
//    public CommonApiResponse<List<TransactionResponseDTO>> getTransactionHistory(String accountNo) {
//        log.info("[CACHE MISS] Fetching transaction history from DB for Account No: {}", accountNo);
//
//        if (!isAccountExistsAndValidate(accountNo)) {
//            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
//        }
//
//        List<Transaction> transactionList = transactionRepo.findByAccount_AccountNo(accountNo);
//
//        List<TransactionResponseDTO> dtoList = transactionList.stream()
//                .map(transaction -> TransactionResponseDTO.builder()
//                        .id(transaction.getId())
//                        .transactionReference(transaction.getTransactionReference())
//                        .amount(String.valueOf(transaction.getAmount()))
//                        .transactionAt(String.valueOf(transaction.getTransactionAt()))
//                        .accountNo(transaction.getAccount().getAccountNo())
//                        .transactionType(transaction.getTransactionType())
//                        .transactionStatus(transaction.getTransactionStatus())
//                        .build())
//                .toList();
//
//        return CommonApiResponse.<List<TransactionResponseDTO>>builder()
//                .message("Transaction history fetched successfully")
//                .statusCode(HttpStatus.OK.value())
//                .data(dtoList)
//                .success(true)
//                .timestamp(LocalDateTime.now())
//                .build();
//    }
//
//    /**
//     * HIGH-CONCURRENCY FINANCIAL TRANSFER ENGINE
//     *
//     * Solution for DB Connection Exhaustion & Deadlocks:
//     * 1. Redisson MultiLock (RAM Queueing): Incoming threads queue up inside Redis memory
//     *    INSTEAD of holding open HikariCP database connections while waiting.
//     * 2. Deterministic Key Sorting: Sorts account keys alphabetically to completely eliminate
//     *    Redis/Database circular deadlocks during concurrent multi-account transactions.
//     * 3. Idempotency Check (Redis SETNX): Prevents replay attacks and double-deductions.
//     * 4. Optimistic Locking (@Version): Replaces heavy SELECT ... FOR UPDATE row locks.
//     * 5. Spring @Retryable: Automatically retries when a version mismatch occurs under surge load.
//     */
//    public CommonApiResponse<TransferResponseDTO> transferAmount(TransferRequestDTO dto) {
//        String redisKey = IDEMPOTENCY_PREFIX + dto.getTransactionReference();
//        log.info("[TRANSFER INITIATED] TxnRef: {} | Sender: {} | Receiver: {} | Amount: {}",
//                dto.getTransactionReference(), dto.getAccountNoFrom(), dto.getAccountNoTo(), dto.getTransferAmount());
//
//        // Step 1: Idempotency Check via SETNX
//        Boolean isFirstRequest = redisTemplate.opsForValue()
//                .setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(5));
//
//        if (Boolean.FALSE.equals(isFirstRequest)) {
//            String currentStatus = redisTemplate.opsForValue().get(redisKey);
//            if ("PROCESSING".equals(currentStatus)) {
//                throw new ConflictException("Transaction is currently being processed. Please wait.");
//            }
//            throw new ConflictException("Transaction has already been processed with this reference.");
//        }
//
//        String accountFrom = dto.getAccountNoFrom();
//        String accountTo = dto.getAccountNoTo();
//
//        if (accountFrom == null || accountTo == null) {
//            redisTemplate.delete(redisKey);
//            throw new IllegalArgumentException("Sender and Receiver accounts must not be null for transfers.");
//        }
//
//        // Step 2: Deterministic Lock Key Ordering (Prevents Deadlocks)
//        String firstLockKey = accountFrom.compareTo(accountTo) < 0 ? accountFrom : accountTo;
//        String secondLockKey = accountFrom.compareTo(accountTo) < 0 ? accountTo : accountFrom;
//
//        RLock lock1 = redissonClient.getLock("lock:account:" + firstLockKey);
//        RLock lock2 = redissonClient.getLock("lock:account:" + secondLockKey);
//        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);
//
//        boolean isLockAcquired = false;
//
//        try {
//            // Step 3: Acquire Distributed Multi-Lock
//            isLockAcquired = multiLock.tryLock(3, 5, TimeUnit.SECONDS);
//            if (!isLockAcquired) {
//                throw new ConflictException("System is busy processing another transaction for these accounts. Please try again.");
//            }
//
//            // Step 4: Execute Transactional DB Transfer
//            CommonApiResponse<TransferResponseDTO> response = executeTransferTransaction(dto);
//
//            // Mark Idempotency SUCCESS in Redis (24h TTL)
//            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofHours(24));
//
//            // Evict Caches
//            evictAccountAndTransactionCaches(accountFrom);
//            evictAccountAndTransactionCaches(accountTo);
//
//            return response;
//
//        } catch ( ConflictException | ResourceNotFoundException | IllegalArgumentException ex) {
//            redisTemplate.delete(redisKey);
//            log.warn("[TRANSFER REJECTED] TxnRef: {} | Reason: {}", dto.getTransactionReference(), ex.getMessage());
//            throw ex;
//        } catch (Exception ex) {
//            redisTemplate.delete(redisKey);
//            log.error("[TRANSFER TECHNICAL FAILURE] TxnRef: {} | Unexpected Error: {}", dto.getTransactionReference(), ex.getMessage(), ex);
//            throw new RuntimeException("Transaction failed due to a system error: " + ex.getMessage(), ex);
//        } finally {
//            if (isLockAcquired && multiLock.isHeldByCurrentThread()) {
//                try {
//                    multiLock.unlock();
//                } catch (Exception e) {
//                    log.error("Error releasing Redisson lock for accounts {} and {}", accountFrom, accountTo, e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Transactional DB Execution Engine with Optimistic Locking Retry.
//     * Separated from Redis lock setup so each retry attempt gets a fresh @Transactional context.
//     */
//    @Retryable(
//            retryFor = { ObjectOptimisticLockingFailureException.class },
//            maxAttempts = 3,
//            backoff = @Backoff(delay = 50, multiplier = 2)
//    )
//    @Transactional(rollbackFor = Exception.class)
//    public CommonApiResponse<TransferResponseDTO> executeTransferTransaction(TransferRequestDTO dto) {
//        Account accountFrom = accountRepo.findByAccountNo(dto.getAccountNoFrom())
//                .orElseThrow(() -> new ResourceNotFoundException("Sender account does not exist or invalid format"));
//
//        Account accountTo = accountRepo.findByAccountNo(dto.getAccountNoTo())
//                .orElseThrow(() -> new ResourceNotFoundException("Receiver account does not exist or invalid format"));
//
//        BigDecimal amount = dto.getTransferAmount();
//
//        if (accountFrom.getBalance().compareTo(amount) < 0) {
//            throw new ResourceNotFoundException("Insufficient balance in sender account.");
//        }
//
//        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
//        accountTo.setBalance(accountTo.getBalance().add(amount));
//
//        accountRepo.save(accountFrom);
//        accountRepo.save(accountTo);
//
//        return CommonApiResponse.<TransferResponseDTO>builder()
//                .success(true)
//                .statusCode(HttpStatus.OK.value())
//                .message("Transfer completed successfully.")
//                .build();
//    }
//
//    private void evictAccountAndTransactionCaches(String accountNo) {
//        Objects.requireNonNull(cacheManager.getCache(RedisCacheConfig.CACHE_ACCOUNT_DETAILS)).evict(accountNo);
//        Objects.requireNonNull(cacheManager.getCache(RedisCacheConfig.CACHE_TRANSACTION_HISTORY)).evict(accountNo);
//        log.debug("[CACHE EVICTED] Cleared accountDetails and transactionHistory for Account: {}", accountNo);
//    }
//}


import com.bluewave.apexbank.account.dto.*;
import com.bluewave.apexbank.config.RedisCacheConfig;
import com.bluewave.apexbank.profiles.ProfileRepo;
import com.bluewave.apexbank.profiles.Profiles;
import com.bluewave.apexbank.transactionlimit.TransactionLimitService;
import com.bluewave.apexbank.transactionlimit.dto.TransactionLimitResponseDTO;
import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.utils.common.*;
import com.bluewave.apexbank.utils.exceptions.ConflictException;
import com.bluewave.apexbank.utils.exceptions.ForbiddenException;
import com.bluewave.apexbank.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;
    private final ProfileRepo profileRepo;
    private final SecurityUtils securityUtils;
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;
    private final TransactionLimitService transactionLimitService;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final String DAILY_COUNT_PREFIX = "limit:daily:count:";

    /**
     * Generates a cryptographically secure 12-digit account number.
     */
    private String generate12DigitAccountNo() {
        SecureRandom random = new SecureRandom();
        long number = 100000000000L + (long)(random.nextDouble() * 900000000000L);
        return String.valueOf(number);
    }

    /**
     * Fast-fail validation to check account number format and existence in DB.
     */
    private boolean isAccountExistsAndValidate(String accountNo) {
        if (accountNo == null || accountNo.length() != 12) {
            return false;
        }
        return accountRepo.existsByAccountNo(accountNo);
    }

    /**
     * Creates a new bank account linked to a user profile.
     */
    @Transactional(rollbackFor = Exception.class)
    public CommonApiResponse<String> createAccount(CreateAccountRequestDTO dto) {
        log.info("[CREATE ACCOUNT] Initializing new account creation for Profile ID: {}", dto.getProfileId());

        String accountNo = generate12DigitAccountNo();

        Profiles profile = profileRepo.findById(dto.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + dto.getProfileId()));

        Account account = new Account();
        account.setAccountNo(accountNo);
        account.setAccountType(dto.getAccountType());
        account.setBalance(dto.getInitialAmount() != null ? dto.getInitialAmount() : BigDecimal.ZERO);
        account.setUsersAccountStatus(UsersAccountStatus.ACTIVE);
        account.setUsers(profile.getUsers());

        accountRepo.save(account);

        log.info("[CREATE ACCOUNT SUCCESS] Successfully generated Account No: {} for Profile ID: {}", accountNo, dto.getProfileId());

        return CommonApiResponse.<String>builder()
                .data("Account Number: " + accountNo)
                .message("New account created successfully")
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * Fetches accounts owned by the currently authenticated customer user.
     */
    @Transactional(readOnly = true)
    public CommonApiResponse<List<AccountResponseDTO>> getMyAccountDetails() {
        Users currentUser = securityUtils.getCurrentUserEntity();
        Set<String> roles = securityUtils.getUsersRoles(currentUser);

        if (!roles.contains("ROLE_CUSTOMER")) {
            throw new ForbiddenException("Role not permitted to access this resource");
        }

        List<Account> accounts = accountRepo.findAllByUsers_Username(currentUser.getUsername());

        if (accounts.isEmpty()) {
            throw new ResourceNotFoundException("No account details found for current user");
        }

        List<AccountResponseDTO> responseDTOs = accounts.stream()
                .map(account -> AccountResponseDTO.builder()
                        .accountNo(account.getAccountNo())
                        .balance(account.getBalance())
                        .accountType(account.getAccountType())
                        .usersAccountStatus(account.getUsersAccountStatus())
                        .username(currentUser.getUsername())
                        .email(currentUser.getEmail())
                        .createdAt(account.getCreatedAt())
                        .build())
                .toList();

        return CommonApiResponse.<List<AccountResponseDTO>>builder()
                .message("Account details fetched successfully")
                .statusCode(HttpStatus.OK.value())
                .data(responseDTOs)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Retrieves account metadata by Account Number. Cached in Redis for 1 Hour.
     */
    @Cacheable(value = RedisCacheConfig.CACHE_ACCOUNT_DETAILS, key = "#accountNo")
    @Transactional(readOnly = true)
    public CommonApiResponse<AccountResponseDTO> getAccountDetailsByAccountNo(String accountNo) {
        log.info("[CACHE MISS] Fetching account details from database for Account No: {}", accountNo);

        if (!isAccountExistsAndValidate(accountNo)) {
            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
        }

        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account details not found"));

        AccountResponseDTO responseDTO = AccountResponseDTO.builder()
                .accountNo(account.getAccountNo())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .usersAccountStatus(account.getUsersAccountStatus())
                .username(account.getUsers() != null ? account.getUsers().getUsername() : null)
                .email(account.getUsers() != null ? account.getUsers().getEmail() : null)
                .createdAt(account.getCreatedAt())
                .build();

        return CommonApiResponse.<AccountResponseDTO>builder()
                .message("Account details fetched successfully")
                .statusCode(HttpStatus.OK.value())
                .data(responseDTO)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Updates an account's operational status (ACTIVE, SUSPENDED, FROZEN). Evicts local account cache.
     */
    @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNT_DETAILS, key = "#dto.accountNo")
    @Transactional(rollbackFor = Exception.class)
    public CommonApiResponse<AccountResponseDTO> updateUserAccountStatus(UpdateUserAccountStatusRequestDTO dto) {
        if (!isAccountExistsAndValidate(dto.getAccountNo())) {
            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
        }

        Account account = accountRepo.findByAccountNo(dto.getAccountNo())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setUsersAccountStatus(dto.getUsersAccountStatus());
        accountRepo.save(account);

        AccountResponseDTO responseDTO = AccountResponseDTO.builder()
                .accountNo(account.getAccountNo())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .usersAccountStatus(account.getUsersAccountStatus())
                .createdAt(account.getCreatedAt())
                .build();

        return CommonApiResponse.<AccountResponseDTO>builder()
                .message("Account status updated successfully")
                .statusCode(HttpStatus.OK.value())
                .data(responseDTO)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Retrieves account transaction history statement. Cached in Redis for 15 Minutes.
     */
    @Cacheable(value = RedisCacheConfig.CACHE_TRANSACTION_HISTORY, key = "#accountNo")
    @Transactional(readOnly = true)
    public CommonApiResponse<List<TransactionResponseDTO>> getTransactionHistory(String accountNo) {
        log.info("[CACHE MISS] Fetching transaction statement from DB for Account No: {}", accountNo);

        if (!isAccountExistsAndValidate(accountNo)) {
            throw new ResourceNotFoundException("Account does not exist or account number format is invalid");
        }

        List<Transaction> transactionList = transactionRepo.findByAccount_AccountNo(accountNo);

        List<TransactionResponseDTO> dtoList = transactionList.stream()
                .map(transaction -> TransactionResponseDTO.builder()
                        .id(transaction.getId())
                        .transactionReference(transaction.getTransactionReference())
                        .amount(String.valueOf(transaction.getAmount()))
                        .transactionAt(String.valueOf(transaction.getTransactionAt()))
                        .accountNo(transaction.getAccount().getAccountNo())
                        .transactionType(transaction.getTransactionType())
                        .transactionStatus(transaction.getTransactionStatus())
                        .build())
                .toList();

        return CommonApiResponse.<List<TransactionResponseDTO>>builder()
                .message("Transaction history fetched successfully")
                .statusCode(HttpStatus.OK.value())
                .data(dtoList)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * HIGH-CONCURRENCY FINANCIAL TRANSFER ENGINE (PRODUCTION READY)
     *
     * Execution Flow & Safety Measures:
     * Step 1: Idempotency Check (Redis SETNX) -> Prevents duplicate requests/replay attacks.
     * Step 2: Pre-Lock Transaction Limits Check -> Fast-fails if transfer amount exceeds single transfer limit.
     * Step 3: Deterministic Lock Sorting -> Alphabetically orders lock keys to eliminate deadlocks.
     * Step 4: Redisson MultiLock Queueing -> Halts threads in Redis RAM instead of blocking DB connections.
     * Step 5: Atomic Daily Count Tracking -> Uses Redis INCR to enforce thread-safe daily transaction count limits.
     * Step 6: Transactional DB Execution -> Executes transfer with @Version optimistic locking retries.
     * Step 7: Cache Eviction & Cleanup -> Evicts account and history cache; updates idempotency status to SUCCESS.
     */
    public CommonApiResponse<TransferResponseDTO> transferAmount(TransferRequestDTO dto) {
        String redisKey = IDEMPOTENCY_PREFIX + dto.getTransactionReference();
        log.info("[TRANSFER INITIATED] TxnRef: {} | Sender: {} | Receiver: {} | Amount: {}",
                dto.getTransactionReference(), dto.getAccountNoFrom(), dto.getAccountNoTo(), dto.getTransferAmount());

        // STEP 1: Idempotency Verification via Redis SETNX (5-minute TTL while processing)
        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(isFirstRequest)) {
            String currentStatus = redisTemplate.opsForValue().get(redisKey);
            if ("PROCESSING".equals(currentStatus)) {
                throw new ConflictException("Transaction is currently being processed. Please wait.");
            }
            throw new ConflictException("Transaction has already been processed with this reference.");
        }

        String accountFrom = dto.getAccountNoFrom();
        String accountTo = dto.getAccountNoTo();

        if (accountFrom == null || accountTo == null) {
            redisTemplate.delete(redisKey);
            throw new IllegalArgumentException("Sender and Receiver accounts must not be null for transfers.");
        }

        // STEP 2: Pre-Lock Single Transaction Limit Validation (Fast-Fail)
        TransactionLimitResponseDTO senderLimit = transactionLimitService.getTransactionLimit(accountFrom).getData();
        if (dto.getTransferAmount().compareTo(senderLimit.getMaxAmountPerTransaction()) > 0) {
            redisTemplate.delete(redisKey);
            throw new IllegalArgumentException("Transfer amount exceeds maximum single transaction limit of $"
                    + senderLimit.getMaxAmountPerTransaction());
        }

        // STEP 3: Deterministic Lock Key Sorting (Prevents Circular Deadlocks)
        String firstLockKey = accountFrom.compareTo(accountTo) < 0 ? accountFrom : accountTo;
        String secondLockKey = accountFrom.compareTo(accountTo) < 0 ? accountTo : accountFrom;

        RLock lock1 = redissonClient.getLock("lock:account:" + firstLockKey);
        RLock lock2 = redissonClient.getLock("lock:account:" + secondLockKey);
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);

        boolean isLockAcquired = false;
        boolean dailyCountIncremented = false;
        String dailyCountKey = DAILY_COUNT_PREFIX + accountFrom + ":" + LocalDate.now();

        try {
            // STEP 4: Acquire Distributed Redisson MultiLock (Wait 3s, Hold 5s)
            isLockAcquired = multiLock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLockAcquired) {
                throw new ConflictException("System is busy processing another transaction for these accounts. Please try again.");
            }

            // STEP 5: Thread-Safe Atomic Daily Transaction Count Enforcement (Redis INCR)
            Long currentDailyCount = redisTemplate.opsForValue().increment(dailyCountKey);
            dailyCountIncremented = true;

            if (currentDailyCount != null && currentDailyCount == 1) {
                redisTemplate.expire(dailyCountKey, Duration.ofDays(2)); // Auto-cleanup counter after 48h
            }

            if (currentDailyCount != null && currentDailyCount > senderLimit.getDailyLimitPerAccount()) {
                throw new IllegalArgumentException("Daily transaction count limit reached ("
                        + senderLimit.getDailyLimitPerAccount() + " transfers/day allowed).");
            }

            // STEP 6: Execute Relational Database Balance Update
            CommonApiResponse<TransferResponseDTO> response = executeTransferTransaction(dto);

            // STEP 7: Finalize Idempotency Key (24h TTL) & Evict Caches
            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofHours(24));
            evictAccountAndTransactionCaches(accountFrom);
            evictAccountAndTransactionCaches(accountTo);

            return response;

        } catch (ConflictException | ResourceNotFoundException | IllegalArgumentException ex) {
            redisTemplate.delete(redisKey);
            // Roll back the atomic counter if limit check or validation failed inside the lock window
            if (dailyCountIncremented) {
                redisTemplate.opsForValue().decrement(dailyCountKey);
            }
            log.warn("[TRANSFER REJECTED] TxnRef: {} | Reason: {}", dto.getTransactionReference(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            redisTemplate.delete(redisKey);
            if (dailyCountIncremented) {
                redisTemplate.opsForValue().decrement(dailyCountKey);
            }
            log.error("[TRANSFER TECHNICAL FAILURE] TxnRef: {} | Unexpected Error: {}", dto.getTransactionReference(), ex.getMessage(), ex);
            throw new RuntimeException("Transaction failed due to a system error: " + ex.getMessage(), ex);
        } finally {
            // STEP 8: Safely Unlock Distributed MultiLock
            if (isLockAcquired && multiLock.isHeldByCurrentThread()) {
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("Error releasing Redisson lock for accounts {} and {}", accountFrom, accountTo, e);
                }
            }
        }
    }

    /**
     * Isolated Relational Database Transaction execution engine.
     * Annotated with @Retryable to automatically recover from optimistic lock contention (@Version) during surges.
     */
    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional(rollbackFor = Exception.class)
    public CommonApiResponse<TransferResponseDTO> executeTransferTransaction(TransferRequestDTO dto) {
        Account accountFrom = accountRepo.findByAccountNo(dto.getAccountNoFrom())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account does not exist or invalid format"));

        Account accountTo = accountRepo.findByAccountNo(dto.getAccountNoTo())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account does not exist or invalid format"));

        BigDecimal amount = dto.getTransferAmount();

        if (accountFrom.getBalance().compareTo(amount) < 0) {
            throw new ResourceNotFoundException("Insufficient balance in sender account.");
        }

        // Deduct sender balance and add to receiver balance
        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));

        accountRepo.save(accountFrom);
        accountRepo.save(accountTo);

        return CommonApiResponse.<TransferResponseDTO>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Transfer completed successfully.")
                .build();
    }

    /**
     * Helper method to clear Redis cache entries for modified accounts.
     */
    private void evictAccountAndTransactionCaches(String accountNo) {
        Objects.requireNonNull(cacheManager.getCache(RedisCacheConfig.CACHE_ACCOUNT_DETAILS)).evict(accountNo);
        Objects.requireNonNull(cacheManager.getCache(RedisCacheConfig.CACHE_TRANSACTION_HISTORY)).evict(accountNo);
        log.debug("[CACHE EVICTED] Cleared accountDetails and transactionHistory for Account: {}", accountNo);
    }

    @Transactional(readOnly = true)
    public CommonApiResponse<List<AccountResponseDTO>> getAllAccounts() {
        List<Account> accountList = accountRepo.findAll();
        List<AccountResponseDTO> dtoList = accountList.stream().map(account -> AccountResponseDTO.builder()
                .accountNo(account.getAccountNo())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .usersAccountStatus(account.getUsersAccountStatus())
                .username(account.getUsers() != null ? account.getUsers().getUsername() : null)
                .email(account.getUsers() != null ? account.getUsers().getEmail() : null)
                .createdAt(account.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return CommonApiResponse.<List<AccountResponseDTO>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("All company accounts retrieved successfully.")
                .data(dtoList)
                .build();
    }
}