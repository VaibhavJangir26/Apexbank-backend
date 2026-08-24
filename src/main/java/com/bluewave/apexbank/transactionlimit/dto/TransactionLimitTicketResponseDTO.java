package com.bluewave.apexbank.transactionlimit.dto;

import com.bluewave.apexbank.transactionlimit.TransactionLimitTicketStatus;
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
public class TransactionLimitTicketResponseDTO {
    private String ticketId;
    private String accountNo;
    private int requestedDailyLimit;
    private BigDecimal requestedTransactionLimit;
    private TransactionLimitTicketStatus status;
    private String message;
    private String rejectionMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
