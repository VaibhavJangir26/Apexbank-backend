package com.bluewave.apexbank.transactionlimit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLimitRepo extends JpaRepository<AccountTransactionLimit, String> {
    Optional<AccountTransactionLimit> findByAccountNo(String accountNo);
}