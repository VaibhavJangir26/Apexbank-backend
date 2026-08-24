package com.bluewave.apexbank.transactionlimit;

import com.bluewave.apexbank.transactionlimit.dto.*;
import com.bluewave.apexbank.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transaction-limit")
public class TransactionLimitController {

    private final TransactionLimitService transactionLimitService;

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CommonApiResponse<TransactionLimitResponseDTO>> setTransactionLimit(
            @Valid @RequestBody TransactionLimitRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionLimitService.setTransactionLimit(dto));
    }

    @PostMapping("/ticket")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<CommonApiResponse<TransactionLimitTicketResponseDTO>> raiseTicketForLimitChange(
            @Valid @RequestBody TransactionLimitUpdateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionLimitService.raiseTicketForLimitChange(dto));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CommonApiResponse<List<TransactionLimitTicketResponseDTO>>> getAllTickets() {
        return ResponseEntity.ok(transactionLimitService.getAllTickets());
    }

    @GetMapping("/tickets/account/{accountNo}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<CommonApiResponse<List<TransactionLimitTicketResponseDTO>>> getCustomerTickets(
            @PathVariable String accountNo) {
        return ResponseEntity.ok(transactionLimitService.getCustomerTickets(accountNo));
    }

    @PatchMapping("/ticket/{ticketId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CommonApiResponse<TransactionLimitTicketResponseDTO>> resolveTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody ResolveTicketRequestDTO dto) {
        return ResponseEntity.ok(transactionLimitService.resolveTicket(ticketId, dto));
    }

    @GetMapping("/{accountNo}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<CommonApiResponse<TransactionLimitResponseDTO>> getTransactionLimit(
            @PathVariable String accountNo) {
        return ResponseEntity.ok(transactionLimitService.getTransactionLimit(accountNo));
    }
}
