package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.AccountType;
import com.bluewave.apexbank.utils.common.UsersAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;


    private String accountNo;
    private BigDecimal balance;
    private UsersAccountStatus usersAccountStatus;
    private AccountType accountType;
    private String username;
    private String email;
    private LocalDateTime createdAt;
}