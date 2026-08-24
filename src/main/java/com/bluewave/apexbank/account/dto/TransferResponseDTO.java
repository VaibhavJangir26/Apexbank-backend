package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.TransactionStatus;
import com.bluewave.apexbank.utils.common.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;

    private String accountNoTo;
    private String accountNoFrom;
    private BigDecimal senderBalanceRemaining;
    private String transactionId;
    private String transactionReference;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private LocalDateTime transactionAt;
}