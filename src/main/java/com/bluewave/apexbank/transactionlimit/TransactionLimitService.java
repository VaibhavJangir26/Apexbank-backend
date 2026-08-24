package com.bluewave.apexbank.transactionlimit;

import com.bluewave.apexbank.account.Account;
import com.bluewave.apexbank.account.AccountRepo;
import com.bluewave.apexbank.config.RedisCacheConfig;
import com.bluewave.apexbank.transactionlimit.dto.*;
import com.bluewave.apexbank.utils.common.CommonApiResponse;
import com.bluewave.apexbank.utils.common.SecurityUtils;
import com.bluewave.apexbank.utils.exceptions.ForbiddenException;
import com.bluewave.apexbank.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

    private final TransactionLimitRepo transactionLimitRepo;
    private final TransactionLimitTicketRepo ticketRepo;
    private final AccountRepo accountRepo;
    private final SecurityUtils securityUtils;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_TRANSACTION_LIMIT, key = "#dto.accountNo")
    public CommonApiResponse<TransactionLimitResponseDTO> setTransactionLimit(TransactionLimitRequestDTO dto) {
        log.info("Admin setting limits for account: {}. Evicting cache...", dto.getAccountNo());

        AccountTransactionLimit limit = transactionLimitRepo.findByAccountNo(dto.getAccountNo())
                .orElseGet(() -> AccountTransactionLimit.builder().accountNo(dto.getAccountNo()).build());

        limit.setDailyLimitPerAccount(dto.getDailyLimitPerAccount());
        limit.setMaxAmountPerTransaction(dto.getMaxAmountPerTransaction());

        AccountTransactionLimit savedLimit = transactionLimitRepo.save(limit);

        return CommonApiResponse.<TransactionLimitResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Transaction limit updated successfully by Admin.")
                .data(mapToResponseDTO(savedLimit, "Custom limit applied"))
                .build();
    }

    @Transactional
    public CommonApiResponse<TransactionLimitTicketResponseDTO> raiseTicketForLimitChange(TransactionLimitUpdateRequestDTO dto) {
        log.info("Customer raising limit change ticket for account: {}", dto.getAccountNo());

        RaiseTransactionLimitTicket ticket = RaiseTransactionLimitTicket.builder()
                .accountNo(dto.getAccountNo())
                .message(dto.getReasonForUpdate())
                .requestedDailyLimit(dto.getNewLimit())
                .requestedTransactionLimit(dto.getNewTransactionLimit())
                .status(TransactionLimitTicketStatus.PENDING)
                .build();

        RaiseTransactionLimitTicket savedTicket = ticketRepo.save(ticket);

        TransactionLimitTicketResponseDTO responseData = mapToTicketResponseDTO(savedTicket);

        return CommonApiResponse.<TransactionLimitTicketResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Ticket created successfully.")
                .data(responseData)
                .build();
    }

    @Transactional(readOnly = true)
    public CommonApiResponse<List<TransactionLimitTicketResponseDTO>> getAllTickets() {
        log.info("Admin fetching all limit change tickets...");

        List<RaiseTransactionLimitTicket> tickets = ticketRepo.findAll();
        List<TransactionLimitTicketResponseDTO> dtoList = tickets.stream()
                .sorted((t1, t2) -> {
                    if (t1.getCreatedAt() == null || t2.getCreatedAt() == null) return 0;
                    return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                })
                .map(this::mapToTicketResponseDTO)
                .collect(Collectors.toList());

        return CommonApiResponse.<List<TransactionLimitTicketResponseDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("All limit tickets retrieved successfully.")
                .data(dtoList)
                .build();
    }

    @Transactional(readOnly = true)
    public CommonApiResponse<List<TransactionLimitTicketResponseDTO>> getCustomerTickets(String accountNo) {
        log.info("Fetching limit change tickets for account: {}", accountNo);

        // Security check for CUSTOMER role
        if (!securityUtils.hasRole("ROLE_ADMIN") && !securityUtils.hasRole("ROLE_MANAGER")) {
            Account account = accountRepo.findByAccountNo(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNo));

            String currentUsername = securityUtils.getCurrentUsername();
            if (account.getUsers() == null || !currentUsername.equals(account.getUsers().getUsername())) {
                throw new ForbiddenException("You do not have permission to view tickets for this account.");
            }
        }

        List<RaiseTransactionLimitTicket> tickets = ticketRepo.findAllByAccountNo(accountNo);
        List<TransactionLimitTicketResponseDTO> dtoList = tickets.stream()
                .sorted((t1, t2) -> {
                    if (t1.getCreatedAt() == null || t2.getCreatedAt() == null) return 0;
                    return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                })
                .map(this::mapToTicketResponseDTO)
                .collect(Collectors.toList());

        return CommonApiResponse.<List<TransactionLimitTicketResponseDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Limit change tickets retrieved successfully.")
                .data(dtoList)
                .build();
    }

    @Transactional
    public CommonApiResponse<TransactionLimitTicketResponseDTO> resolveTicket(String ticketId, ResolveTicketRequestDTO dto) {
        log.info("Admin resolving ticket ID: {} with status: {}", ticketId, dto.getStatus());

        RaiseTransactionLimitTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Limit ticket not found with ID: " + ticketId));

        if (dto.getStatus() == TransactionLimitTicketStatus.APPROVED) {
            // Apply new limits to account
            AccountTransactionLimit limit = transactionLimitRepo.findByAccountNo(ticket.getAccountNo())
                    .orElseGet(() -> AccountTransactionLimit.builder().accountNo(ticket.getAccountNo()).build());

            limit.setDailyLimitPerAccount(ticket.getRequestedDailyLimit());
            limit.setMaxAmountPerTransaction(ticket.getRequestedTransactionLimit());
            transactionLimitRepo.save(limit);

            // Evict Redis cache
            if (cacheManager.getCache(RedisCacheConfig.CACHE_TRANSACTION_LIMIT) != null) {
                Objects.requireNonNull(cacheManager.getCache(RedisCacheConfig.CACHE_TRANSACTION_LIMIT)).evict(ticket.getAccountNo());
            }

            ticket.setStatus(TransactionLimitTicketStatus.APPROVED);
            ticket.setRejectionMessage(null);
            log.info("Ticket {} approved. Applied daily count: {}, max amount: {}",
                    ticketId, ticket.getRequestedDailyLimit(), ticket.getRequestedTransactionLimit());
        } else if (dto.getStatus() == TransactionLimitTicketStatus.REJECTED) {
            ticket.setStatus(TransactionLimitTicketStatus.REJECTED);
            ticket.setRejectionMessage(dto.getRejectionMessage());
            log.info("Ticket {} rejected. Reason: {}", ticketId, dto.getRejectionMessage());
        }

        RaiseTransactionLimitTicket updatedTicket = ticketRepo.save(ticket);

        return CommonApiResponse.<TransactionLimitTicketResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Ticket resolved successfully as " + dto.getStatus())
                .data(mapToTicketResponseDTO(updatedTicket))
                .build();
    }

    @Cacheable(value = RedisCacheConfig.CACHE_TRANSACTION_LIMIT, key = "#accountNo")
    public CommonApiResponse<TransactionLimitResponseDTO> getTransactionLimit(String accountNo) {
        log.info("Fetching transaction limit from DB for account: {}", accountNo);

        AccountTransactionLimit limit = transactionLimitRepo.findByAccountNo(accountNo)
                .orElseGet(() -> AccountTransactionLimit.builder()
                        .accountNo(accountNo)
                        .dailyLimitPerAccount(10) // System Default
                        .maxAmountPerTransaction(BigDecimal.valueOf(10000.00)) // System Default
                        .build());

        String message = limit.getId() == null ? "Default system limits applied" : "Custom limits applied";

        return CommonApiResponse.<TransactionLimitResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Limits retrieved successfully")
                .data(mapToResponseDTO(limit, message))
                .build();
    }

    private TransactionLimitResponseDTO mapToResponseDTO(AccountTransactionLimit limit, String message) {
        return TransactionLimitResponseDTO.builder()
                .id(limit.getId())
                .accountNo(limit.getAccountNo())
                .dailyLimitPerAccount(limit.getDailyLimitPerAccount())
                .maxAmountPerTransaction(limit.getMaxAmountPerTransaction())
                .message(message)
                .updatedAt(limit.getUpdatedAt())
                .build();
    }

    private TransactionLimitTicketResponseDTO mapToTicketResponseDTO(RaiseTransactionLimitTicket ticket) {
        return TransactionLimitTicketResponseDTO.builder()
                .ticketId(ticket.getId())
                .accountNo(ticket.getAccountNo())
                .requestedDailyLimit(ticket.getRequestedDailyLimit())
                .requestedTransactionLimit(ticket.getRequestedTransactionLimit())
                .status(ticket.getStatus())
                .message(ticket.getMessage())
                .rejectionMessage(ticket.getRejectionMessage())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
