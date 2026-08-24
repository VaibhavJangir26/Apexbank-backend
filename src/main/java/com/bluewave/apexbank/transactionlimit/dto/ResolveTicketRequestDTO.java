package com.bluewave.apexbank.transactionlimit.dto;

import com.bluewave.apexbank.transactionlimit.TransactionLimitTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResolveTicketRequestDTO {
    @NotNull(message = "Status is required (APPROVED or REJECTED)")
    private TransactionLimitTicketStatus status;

    private String rejectionMessage;
}
