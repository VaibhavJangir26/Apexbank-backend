package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequestDTO {

    @NotBlank(message = "Profile ID is required")
    private String profileId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial amount is required")
    @DecimalMin(value = "0.00", message = "Initial amount cannot be negative")
    private BigDecimal initialAmount;
}