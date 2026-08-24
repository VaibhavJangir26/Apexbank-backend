package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferRequestDTO {

    @NotBlank(message = "Profile ID is required")
    private String profileId;

    @NotBlank(message = "Idempotency key / transaction reference is required")
    @Pattern(
            regexp = "^TNX-APEXBANK-[a-zA-Z0-9-]+$",
            message = "Transaction reference must follow format 'TNX-APEXBANK-<key>'"
    )
    private String transactionReference;

    @NotBlank(message = "Receiver account number is required")
    @Size(min = 12, max = 12, message = "12-digit account number is required")
    private String accountNoTo;

    @NotBlank(message = "Sender account number is required")
    @Size(min = 12, max = 12, message = "12-digit account number is required")
    private String accountNoFrom;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal transferAmount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
}