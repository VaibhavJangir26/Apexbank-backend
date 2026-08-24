package com.bluewave.apexbank.account.dto;

import com.bluewave.apexbank.utils.common.TransactionStatus;
import com.bluewave.apexbank.utils.common.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDTO {
    private String id;
    private String transactionReference;
    private String amount;
    private String transactionAt;
    private String accountNo;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
}
