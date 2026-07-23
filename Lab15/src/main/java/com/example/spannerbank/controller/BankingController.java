package com.example.spannerbank.controller;

import com.example.spannerbank.model.Account;
import com.example.spannerbank.model.Customer;
import com.example.spannerbank.model.PaymentTransaction;
import com.example.spannerbank.service.BankingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/banking")
@RequiredArgsConstructor
public class BankingController {

    private final BankingService bankingService;

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(bankingService.getAllCustomers());
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<Account>> getAccountsByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(bankingService.getAccountsByCustomer(customerId));
    }

    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@RequestBody CreateCustomerRequest request) {
        Customer customer = bankingService.createCustomerWithAccount(
                request.getFullName(),
                request.getEmail(),
                request.getAccountType(),
                request.getInitialBalance()
        );
        return ResponseEntity.ok(customer);
    }

    @PostMapping("/transfer")
    public ResponseEntity<PaymentTransaction> transferMoney(@RequestBody TransferRequest request) {
        PaymentTransaction transaction = bankingService.transferMoney(
                request.getSourceAccountId(),
                request.getTargetAccountId(),
                request.getAmount()
        );
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedData(@RequestParam(name = "count", defaultValue = "50") int count) {
        List<Customer> seeded = bankingService.seedBulkCustomers(count);
        return ResponseEntity.ok("Successfully seeded " + seeded.size() + " customers AND " + seeded.size() + " bank accounts in Spanner via PGAdapter!");
    }

    @PostMapping("/accounts/bulk-update")
    public ResponseEntity<String> bulkUpdateAccountTypes(
            @RequestParam(name = "oldType") String oldType,
            @RequestParam(name = "newType") String newType) {
        int updatedCount = bankingService.bulkUpdateAccountTypes(oldType, newType);
        return ResponseEntity.ok("Partitioned DML updated " + updatedCount + " account records from " + oldType + " to " + newType);
    }

    @Data
    public static class CreateCustomerRequest {
        private String fullName;
        private String email;
        private String accountType;
        private BigDecimal initialBalance;
    }

    @Data
    public static class TransferRequest {
        private String sourceAccountId;
        private String targetAccountId;
        private BigDecimal amount;
    }
}
