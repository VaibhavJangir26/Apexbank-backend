package com.bluewave.apexbank.account;

import com.bluewave.apexbank.account.dto.*;
import com.bluewave.apexbank.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<CommonApiResponse<String>> createAccount(@Valid @RequestBody CreateAccountRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(dto));
    }


    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CommonApiResponse<List<AccountResponseDTO>>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }



    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<CommonApiResponse<List<AccountResponseDTO>>> getMyAccountDetails() {
        return ResponseEntity.ok(accountService.getMyAccountDetails());
    }

    @GetMapping("/{accountNo}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<CommonApiResponse<AccountResponseDTO>> getAccountDetailsByAccountNo(@PathVariable String accountNo) {
        return ResponseEntity.ok(accountService.getAccountDetailsByAccountNo(accountNo));
    }

    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CommonApiResponse<AccountResponseDTO>> updateUserAccountStatus(@Valid @RequestBody UpdateUserAccountStatusRequestDTO dto) {
        return ResponseEntity.ok(accountService.updateUserAccountStatus(dto));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<CommonApiResponse<TransferResponseDTO>> transferAmount(@Valid @RequestBody TransferRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.transferAmount(dto));
    }

    @GetMapping("/{accountNo}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<CommonApiResponse<List<TransactionResponseDTO>>> getTransactionHistory(@PathVariable String accountNo) {
        return ResponseEntity.ok(accountService.getTransactionHistory(accountNo));
    }

}