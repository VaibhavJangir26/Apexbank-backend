////package com.bluewave.apexbank;
////
////import com.bluewave.apexbank.account.AccountRepo;
////import com.bluewave.apexbank.account.AccountService;
////import com.bluewave.apexbank.account.dto.TransferRequestDTO;
////import com.bluewave.apexbank.utils.common.TransactionType;
////import org.junit.jupiter.api.BeforeEach;
////import org.junit.jupiter.api.DisplayName;
////import org.junit.jupiter.api.Test;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.boot.test.context.SpringBootTest;
////import org.springframework.cache.Cache;
////import org.springframework.cache.CacheManager;
////import org.springframework.data.redis.core.StringRedisTemplate;
////import org.springframework.data.redis.core.ValueOperations;
////import org.springframework.test.context.ActiveProfiles;
////import org.springframework.test.context.bean.override.mockito.MockitoBean;
////
////import java.math.BigDecimal;
////import java.math.RoundingMode;
////import java.time.Duration;
////import java.util.List;
////import java.util.Random;
////import java.util.UUID;
////import java.util.concurrent.CountDownLatch;
////import java.util.concurrent.ExecutorService;
////import java.util.concurrent.Executors;
////import java.util.concurrent.TimeUnit;
////import java.util.concurrent.atomic.AtomicInteger;
////import java.util.concurrent.atomic.AtomicReference;
////
////import static org.junit.jupiter.api.Assertions.assertEquals;
////import static org.mockito.ArgumentMatchers.any;
////import static org.mockito.ArgumentMatchers.anyString;
////import static org.mockito.Mockito.when;
////
////@SpringBootTest
////@ActiveProfiles("test")
////public class ConcurrentTransfer50ThreadTest {
////
////    private static final Logger log = LoggerFactory.getLogger(ConcurrentTransfer50ThreadTest.class);
////
////    @Autowired
////    private AccountService accountService;
////
////    @Autowired
////    private AccountRepo accountRepo;
////
////    // Mock Redis & Cache dependencies required by AccountService
////    @MockitoBean
////    private StringRedisTemplate redisTemplate;
////
////    @MockitoBean
////    private ValueOperations<String, String> valueOperations;
////
////    @MockitoBean
////    private CacheManager cacheManager;
////
////    @MockitoBean
////    private Cache cache;
////
////    private final String ACCOUNT_1 = "346503591595";
////    private final String ACCOUNT_2 = "665671767420";
////    private final String ACCOUNT_3 = "695116668701";
////
////    @BeforeEach
////    void setupMocks() {
////        // Mock Redis calls to prevent NullPointerExceptions during transfer/transaction execution
////        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
////        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
////
////        // Mock CacheManager calls for cache eviction
////        when(cacheManager.getCache(anyString())).thenReturn(cache);
////    }
////
////    @Test
////    @DisplayName("50 Concurrent Threads - Complex Multi-Operation (Credit, Debit, Transfer) Financial Integrity Test")
////    void execute50ComplexConcurrentTransactions() throws InterruptedException {
////        int totalThreads = 50;
////        ExecutorService executorService = Executors.newFixedThreadPool(10); // Capped to Hikari Max Connection Pool
////        CountDownLatch startSignal = new CountDownLatch(1);                  // Holds all threads for simultaneous firing
////        CountDownLatch doneSignal = new CountDownLatch(totalThreads);        // Tracks completion of all tasks
////
////        // Transaction Execution Counters
////        AtomicInteger successTransferCount = new AtomicInteger(0);
////        AtomicInteger successDebitCount = new AtomicInteger(0);
////        AtomicInteger successCreditCount = new AtomicInteger(0);
////        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
////        AtomicInteger systemErrorCount = new AtomicInteger(0);
////
////        // Thread-Safe Financial Accumulators
////        AtomicReference<BigDecimal> totalCreditedAmount = new AtomicReference<>(BigDecimal.ZERO);
////        AtomicReference<BigDecimal> totalDebitedAmount = new AtomicReference<>(BigDecimal.ZERO);
////
////        List<String> accountPool = List.of(ACCOUNT_1, ACCOUNT_2, ACCOUNT_3);
////        List<TransactionType> availableTypes = List.of(TransactionType.TRANSFER, TransactionType.DEBIT, TransactionType.CREDIT);
////        Random random = new Random();
////
////        // 1. Fetch & Log Initial Balances
////        BigDecimal initialBal1 = accountRepo.findByAccountNo(ACCOUNT_1)
////                .orElseThrow(() -> new IllegalStateException("Account missing: " + ACCOUNT_1)).getBalance();
////        BigDecimal initialBal2 = accountRepo.findByAccountNo(ACCOUNT_2)
////                .orElseThrow(() -> new IllegalStateException("Account missing: " + ACCOUNT_2)).getBalance();
////        BigDecimal initialBal3 = accountRepo.findByAccountNo(ACCOUNT_3)
////                .orElseThrow(() -> new IllegalStateException("Account missing: " + ACCOUNT_3)).getBalance();
////
////        BigDecimal initialTotalPool = initialBal1.add(initialBal2).add(initialBal3);
////
////        log.info("=================== INITIAL BANK STATE ===================");
////        log.info("Account 1 ({}) Initial Balance: ${}", ACCOUNT_1, initialBal1);
////        log.info("Account 2 ({}) Initial Balance: ${}", ACCOUNT_2, initialBal2);
////        log.info("Account 3 ({}) Initial Balance: ${}", ACCOUNT_3, initialBal3);
////        log.info("TOTAL INITIAL MONEY IN SYSTEM POOL : ${}", initialTotalPool);
////        log.info("==========================================================");
////
////        // 2. Queue 50 Concurrent Multi-Operation Tasks
////        for (int i = 1; i <= totalThreads; i++) {
////            final int taskId = i;
////
////            // Pick Random Operation Type: TRANSFER, DEBIT, or CREDIT
////            TransactionType type = availableTypes.get(random.nextInt(availableTypes.size()));
////
////            // Generate Random Amount between $10.00 and $500.00
////            BigDecimal randomAmount = BigDecimal.valueOf(10 + random.nextInt(491))
////                    .setScale(2, RoundingMode.HALF_UP);
////
////            // Select Target Accounts
////            String primaryAcc = accountPool.get(random.nextInt(accountPool.size()));
////            String secondaryAcc;
////            do {
////                secondaryAcc = accountPool.get(random.nextInt(accountPool.size()));
////            } while (primaryAcc.equals(secondaryAcc));
////
////            final String fromAcc;
////            final String toAcc;
////
////            // Structure DTO according to Operation Type
////            switch (type) {
////                case CREDIT -> {
////                    fromAcc = null;
////                    toAcc = primaryAcc;
////                }
////                case DEBIT -> {
////                    fromAcc = primaryAcc;
////                    toAcc = null;
////                }
////                default -> { // TRANSFER
////                    fromAcc = primaryAcc;
////                    toAcc = secondaryAcc;
////                }
////            }
////
////            executorService.submit(() -> {
////                try {
////                    startSignal.await(); // Hold until start signal is triggered
////
////                    TransferRequestDTO request = TransferRequestDTO.builder()
////                            .accountNoFrom(fromAcc)
////                            .accountNoTo(toAcc)
////                            .transferAmount(randomAmount)
////                            .transactionType(type)
////                            .transactionReference("TXN-COMPLEX-50-" + taskId + "-" + UUID.randomUUID())
////                            .build();
////
////                    accountService.transferAmount(request);
////
////                    // Track metrics upon successful execution
////                    switch (type) {
////                        case TRANSFER -> {
////                            successTransferCount.incrementAndGet();
////                            log.info("[THREAD #{}] [TRANSFER SUCCESS] {} -> {} | Amount: ${}", taskId, fromAcc, toAcc, randomAmount);
////                        }
////                        case DEBIT -> {
////                            successDebitCount.incrementAndGet();
////                            totalDebitedAmount.accumulateAndGet(randomAmount, BigDecimal::add);
////                            log.info("[THREAD #{}] [DEBIT SUCCESS] Account: {} | Amount Withdrawn: -${}", taskId, fromAcc, randomAmount);
////                        }
////                        case CREDIT -> {
////                            successCreditCount.incrementAndGet();
////                            totalCreditedAmount.accumulateAndGet(randomAmount, BigDecimal::add);
////                            log.info("[THREAD #{}] [CREDIT SUCCESS] Account: {} | Amount Deposited: +${}", taskId, toAcc, randomAmount);
////                        }
////                    }
////
////                } catch (Exception e) {
////                    if (e.getMessage() != null && e.getMessage().contains("Insufficient balance")) {
////                        insufficientFundsCount.incrementAndGet();
////                        log.warn("[THREAD #{}] REJECTED (Insufficient Funds) [{}] Account: {} | Attempted: ${}",
////                                taskId, type, fromAcc, randomAmount);
////                    } else {
////                        systemErrorCount.incrementAndGet();
////                        log.error("[THREAD #{}] TECHNICAL FAILURE [{}] | Reason: {}", taskId, type, e.getMessage(), e);
////                    }
////                } finally {
////                    doneSignal.countDown();
////                }
////            });
////        }
////
////        // 3. Trigger Concurrent Execution
////        long startTime = System.currentTimeMillis();
////        log.info("FIRING 50 CONCURRENT MULTI-OPERATION THREADS...");
////        startSignal.countDown();
////
////        boolean finishedInTime = doneSignal.await(60, TimeUnit.SECONDS);
////        long elapsedTime = System.currentTimeMillis() - startTime;
////
////        executorService.shutdown();
////
////        // 4. Fetch Final Balances from DB
////        BigDecimal finalBal1 = accountRepo.findByAccountNo(ACCOUNT_1).orElseThrow().getBalance();
////        BigDecimal finalBal2 = accountRepo.findByAccountNo(ACCOUNT_2).orElseThrow().getBalance();
////        BigDecimal finalBal3 = accountRepo.findByAccountNo(ACCOUNT_3).orElseThrow().getBalance();
////        BigDecimal finalTotalPool = finalBal1.add(finalBal2).add(finalBal3);
////
////        // Financial Conservation Equation: Expected Pool = Initial Pool + Total Injected (Credits) - Total Withdrawn (Debits)
////        BigDecimal expectedFinalTotal = initialTotalPool
////                .add(totalCreditedAmount.get())
////                .subtract(totalDebitedAmount.get());
////
////        int totalSuccessful = successTransferCount.get() + successDebitCount.get() + successCreditCount.get();
////        int totalProcessed = totalSuccessful + insufficientFundsCount.get() + systemErrorCount.get();
////
////        // 5. Output Comprehensive Audit Report
////        log.info("==========================================================================================");
////        log.info("                         FINAL CONCURRENCY & AUDIT REPORT                                 ");
////        log.info("==========================================================================================");
////        log.info("Execution Status             : {}", finishedInTime ? "PASSED (Completed in Time)" : "FAILED (Timed Out)");
////        log.info("Total Processing Duration    : {} ms", elapsedTime);
////        log.info("------------------------------------------------------------------------------------------");
////        log.info("TRANSACTION EXECUTION BREAKDOWN:");
////        log.info("  • Successful Transfers     : {}", successTransferCount.get());
////        log.info("  • Successful Debits        : {}", successDebitCount.get());
////        log.info("  • Successful Credits       : {}", successCreditCount.get());
////        log.info("  • Insufficient Fund Rejects: {}", insufficientFundsCount.get());
////        log.info("  • System / Deadlock Errors : {}", systemErrorCount.get());
////        log.info("  • Total Tasks Processed    : {} / {}", totalProcessed, totalThreads);
////        log.info("------------------------------------------------------------------------------------------");
////        log.info("FINANCIAL AUDIT & CASH FLOW MONITOR:");
////        log.info("  • Total Money Injected (Credits) : +${}", totalCreditedAmount.get());
////        log.info("  • Total Money Withdrawn (Debits)  : -${}", totalDebitedAmount.get());
////        log.info("  • Net Cash Flow Delta            : ${}", totalCreditedAmount.get().subtract(totalDebitedAmount.get()));
////        log.info("------------------------------------------------------------------------------------------");
////        log.info("ACCOUNT BALANCES:");
////        log.info("  • Account 1 ({}) Final Balance: ${}", ACCOUNT_1, finalBal1);
////        log.info("  • Account 2 ({}) Final Balance: ${}", ACCOUNT_2, finalBal2);
////        log.info("  • Account 3 ({}) Final Balance: ${}", ACCOUNT_3, finalBal3);
////        log.info("------------------------------------------------------------------------------------------");
////        log.info("POOL INTEGRITY VERIFICATION:");
////        log.info("  • Initial Total System Pool : ${}", initialTotalPool);
////        log.info("  • Expected Final Pool       : ${}", expectedFinalTotal);
////        log.info("  • Actual Final System Pool  : ${}", finalTotalPool);
////        log.info("  • Discrepancy / Variance    : ${}", finalTotalPool.subtract(expectedFinalTotal));
////        log.info("==========================================================================================");
////
////        // 6. Strict Financial & Concurrency Assertions
////        assertEquals(totalThreads, totalProcessed, "All 50 threads must execute to completion.");
////        assertEquals(0, systemErrorCount.get(), "Zero technical errors or database deadlocks allowed.");
////        assertEquals(0, expectedFinalTotal.compareTo(finalTotalPool),
////                "CRITICAL: System Money Conservation Law violated! Actual pool balance does not match net expected cash flow.");
////    }
////}
//package com.bluewave.apexbank;
//
//import com.bluewave.apexbank.account.AccountRepo;
//import com.bluewave.apexbank.account.AccountService;
//import com.bluewave.apexbank.account.dto.TransferRequestDTO;
//import com.bluewave.apexbank.utils.common.TransactionType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.ValueOperations;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.Duration;
//import java.util.List;
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//@ActiveProfiles("test")
//public class ConcurrentTransfer50ThreadTest {
//
//    private static final Logger log = LoggerFactory.getLogger(ConcurrentTransfer50ThreadTest.class);
//
//    @Autowired
//    private AccountService accountService;
//
//    @Autowired
//    private AccountRepo accountRepo;
//
//    @MockitoBean
//    private StringRedisTemplate redisTemplate;
//
//    @MockitoBean
//    private ValueOperations<String, String> valueOperations;
//
//    @MockitoBean
//    private CacheManager cacheManager;
//
//    @MockitoBean
//    private Cache cache;
//
//    // Accounts 1, 3, and 4 from Database
//    private final String ACCOUNT_1 = "346503591595";
//    private final String ACCOUNT_3 = "695116668701";
//    private final String ACCOUNT_4 = "665671767420";
//
//    @BeforeEach
//    void setupMocks() {
//        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
//        when(cacheManager.getCache(anyString())).thenReturn(cache);
//    }
//
//    @Test
//    @DisplayName("50 Concurrent Threads - Pure Transfer Financial Integrity Test")
//    void execute50PureConcurrentTransfers() throws InterruptedException {
//        int totalThreads = 50;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch startSignal = new CountDownLatch(1);
//        CountDownLatch doneSignal = new CountDownLatch(totalThreads);
//
//        AtomicInteger successTransferCount = new AtomicInteger(0);
//        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
//        AtomicInteger lockConflictCount = new AtomicInteger(0);
//        AtomicInteger systemErrorCount = new AtomicInteger(0);
//
//        List<String> accountPool = List.of(ACCOUNT_1, ACCOUNT_3, ACCOUNT_4);
//        Random random = new Random();
//
//        BigDecimal initialBal1 = accountRepo.findByAccountNo(ACCOUNT_1).orElseThrow().getBalance();
//        BigDecimal initialBal3 = accountRepo.findByAccountNo(ACCOUNT_3).orElseThrow().getBalance();
//        BigDecimal initialBal4 = accountRepo.findByAccountNo(ACCOUNT_4).orElseThrow().getBalance();
//        BigDecimal initialTotalPool = initialBal1.add(initialBal3).add(initialBal4);
//
//        log.info("=================== INITIAL BANK STATE ===================");
//        log.info("Account 1 ({}) Initial Balance: ${}", ACCOUNT_1, initialBal1);
//        log.info("Account 3 ({}) Initial Balance: ${}", ACCOUNT_3, initialBal3);
//        log.info("Account 4 ({}) Initial Balance: ${}", ACCOUNT_4, initialBal4);
//        log.info("TOTAL INITIAL SYSTEM POOL            : ${}", initialTotalPool);
//        log.info("==========================================================");
//
//        for (int i = 1; i <= totalThreads; i++) {
//            final int taskId = i;
//            BigDecimal randomAmount = BigDecimal.valueOf(10 + random.nextInt(491)).setScale(2, RoundingMode.HALF_UP);
//
//            String sender = accountPool.get(random.nextInt(accountPool.size()));
//            String receiver;
//            do {
//                receiver = accountPool.get(random.nextInt(accountPool.size()));
//            } while (sender.equals(receiver));
//
//            final String fromAcc = sender;
//            final String toAcc = receiver;
//
//            executorService.submit(() -> {
//                try {
//                    startSignal.await();
//
//                    TransferRequestDTO request = TransferRequestDTO.builder()
//                            .accountNoFrom(fromAcc)
//                            .accountNoTo(toAcc)
//                            .transferAmount(randomAmount)
//                            .transactionType(TransactionType.TRANSFER)
//                            .transactionReference("TXN-TRANSFER-50-" + taskId + "-" + UUID.randomUUID())
//                            .build();
//
//                    accountService.transferAmount(request);
//                    successTransferCount.incrementAndGet();
//                    log.info("[THREAD #{}] [TRANSFER SUCCESS] {} -> {} | Amount: ${}", taskId, fromAcc, toAcc, randomAmount);
//
//                } catch (Exception e) {
//                    String errorDetails = e.toString();
//                    if (e.getCause() != null) {
//                        errorDetails += " | Cause: " + e.getCause().toString();
//                    }
//
//                    if (errorDetails.toLowerCase().contains("insufficient balance")) {
//                        insufficientFundsCount.incrementAndGet();
//                        log.warn("[THREAD #{}] REJECTED (Insufficient Balance) {} -> {} | Amount: ${}", taskId, fromAcc, toAcc, randomAmount);
//                    } else if (errorDetails.toLowerCase().contains("system is busy") || errorDetails.contains("ConflictException")) {
//                        lockConflictCount.incrementAndGet();
//                        log.warn("[THREAD #{}] REJECTED (Concurrency Lock Busy) {} -> {}", taskId, fromAcc, toAcc);
//                    } else {
//                        systemErrorCount.incrementAndGet();
//                        log.error("[THREAD #{}] TECHNICAL FAILURE | Reason: {}", taskId, errorDetails, e);
//                    }
//                } finally {
//                    doneSignal.countDown();
//                }
//            });
//        }
//
//        long startTime = System.currentTimeMillis();
//        log.info("FIRING 50 CONCURRENT TRANSFER THREADS...");
//        startSignal.countDown();
//
//        boolean finishedInTime = doneSignal.await(60, TimeUnit.SECONDS);
//        long elapsedTime = System.currentTimeMillis() - startTime;
//        executorService.shutdown();
//
//        BigDecimal finalBal1 = accountRepo.findByAccountNo(ACCOUNT_1).orElseThrow().getBalance();
//        BigDecimal finalBal3 = accountRepo.findByAccountNo(ACCOUNT_3).orElseThrow().getBalance();
//        BigDecimal finalBal4 = accountRepo.findByAccountNo(ACCOUNT_4).orElseThrow().getBalance();
//        BigDecimal finalTotalPool = finalBal1.add(finalBal3).add(finalBal4);
//
//        int totalProcessed = successTransferCount.get() + insufficientFundsCount.get()
//                + lockConflictCount.get() + systemErrorCount.get();
//
//        log.info("==========================================================================================");
//        log.info("                         FINAL CONCURRENCY & AUDIT REPORT                                 ");
//        log.info("==========================================================================================");
//        log.info("Execution Status             : {}", finishedInTime ? "PASSED (Completed in Time)" : "FAILED (Timed Out)");
//        log.info("Total Processing Duration    : {} ms", elapsedTime);
//        log.info("------------------------------------------------------------------------------------------");
//        log.info("TRANSACTION BREAKDOWN:");
//        log.info("  • Successful Transfers     : {}", successTransferCount.get());
//        log.info("  • Insufficient Fund Rejects: {}", insufficientFundsCount.get());
//        log.info("  • Concurrency Lock Blocks  : {}", lockConflictCount.get());
//        log.info("  • Technical System Errors  : {}", systemErrorCount.get());
//        log.info("  • Total Tasks Processed    : {} / {}", totalProcessed, totalThreads);
//        log.info("------------------------------------------------------------------------------------------");
//        log.info("ACCOUNT BALANCES:");
//        log.info("  • Account 1 ({}) Balance    : ${}", ACCOUNT_1, finalBal1);
//        log.info("  • Account 3 ({}) Balance    : ${}", ACCOUNT_3, finalBal3);
//        log.info("  • Account 4 ({}) Balance    : ${}", ACCOUNT_4, finalBal4);
//        log.info("------------------------------------------------------------------------------------------");
//        log.info("POOL INTEGRITY VERIFICATION:");
//        log.info("  • Initial Total System Pool : ${}", initialTotalPool);
//        log.info("  • Actual Final System Pool  : ${}", finalTotalPool);
//        log.info("  • Discrepancy / Variance    : ${}", finalTotalPool.subtract(initialTotalPool));
//        log.info("==========================================================================================");
//
//        assertEquals(totalThreads, totalProcessed, "All 50 threads must execute to completion.");
//        assertEquals(0, systemErrorCount.get(), "Zero technical system errors allowed.");
//        assertEquals(0, initialTotalPool.compareTo(finalTotalPool), "Conservation of money law: Pool total must remain unchanged.");
//    }
//}