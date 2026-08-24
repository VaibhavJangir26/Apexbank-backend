package com.bluewave.apexbank.transactionlimit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionLimitUpdateRequestDTO {
    @NotBlank(message = "Account number is required")
    private String accountNo;

    @NotBlank(message = "Reason for update is required")
    private String reasonForUpdate;

    @Min(value = 11, message = "New daily transaction limit count must be greater than default (10)")
    private int newLimit;

    @DecimalMin(value = "10000.01", message = "New transaction limit must be greater than default (10000.00)")
    private BigDecimal newTransactionLimit;
}