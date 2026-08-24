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
@Table(name = "raise_transaction_limit_ticket", indexes = {@Index(name = "idx_ticket_account_no", columnList = "accountNo")})
public class RaiseTransactionLimitTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountNo;

    private String message;

    private String rejectionMessage;

    @Column(nullable = false)
    private int requestedDailyLimit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedTransactionLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionLimitTicketStatus status = TransactionLimitTicketStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}