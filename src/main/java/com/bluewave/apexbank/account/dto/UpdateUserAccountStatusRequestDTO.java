package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.UsersAccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserAccountStatusRequestDTO {

    @NotBlank(message = "Account number is required")
    @Size(min = 12, max = 12, message = "12-digit account number is required")
    private String accountNo;

    @NotNull(message = "User account status is required")
    private UsersAccountStatus usersAccountStatus;
}