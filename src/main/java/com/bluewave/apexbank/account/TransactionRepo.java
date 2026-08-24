package com.bluewave.apexbank.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccount_AccountNo(String accountNo);
}