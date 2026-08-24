package com.bluewave.apexbank.transactionlimit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionLimitRequestDTO {
    @NotBlank(message = "Account number is required")
    private String accountNo;

    @Min(value = 1, message = "Daily transaction count limit must be at least 1")
    private int dailyLimitPerAccount;

    @NotNull(message = "Max amount per transaction is required")
    @DecimalMin(value = "1.00", message = "Amount must be greater than zero")
    private BigDecimal maxAmountPerTransaction;
}