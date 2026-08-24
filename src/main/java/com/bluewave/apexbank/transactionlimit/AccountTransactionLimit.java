package com.bluewave.apexbank.transactionlimit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "account_transaction_limit", indexes = {@Index(name = "idx_account_no", columnList = "accountNo", unique = true)})
public class AccountTransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String accountNo;

    @Builder.Default
    @Column(nullable = false)
    private int dailyLimitPerAccount = 10;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmountPerTransaction = BigDecimal.valueOf(10000.00);

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}