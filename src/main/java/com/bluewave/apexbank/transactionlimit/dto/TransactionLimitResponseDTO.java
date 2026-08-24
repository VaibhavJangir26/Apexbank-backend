package com.bluewave.apexbank.transactionlimit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionLimitResponseDTO {
    private String id;
    private String accountNo;
    private String message;
    private int dailyLimitPerAccount;
    private BigDecimal maxAmountPerTransaction;
    private LocalDateTime updatedAt;
}