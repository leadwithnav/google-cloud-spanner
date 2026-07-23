package com.example.spannerbank.service;

import com.example.spannerbank.model.Account;
import com.example.spannerbank.model.Customer;
import com.example.spannerbank.model.PaymentTransaction;
import com.example.spannerbank.repository.AccountRepository;
import com.example.spannerbank.repository.CustomerRepository;
import com.example.spannerbank.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    // =========================================================================
    // BEST PRACTICE 1: Non-Locking Snapshot Read
    // Serves read-only queries from nearest read-replicas without acquiring locks
    // =========================================================================
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        log.info("Executing Snapshot Read for all customers...");
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByCustomer(String customerId) {
        log.info("Executing Snapshot Read for interleaved accounts of customer: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    // =========================================================================
    // BEST PRACTICE 2: Physical Table Interleaving (INTERLEAVE IN PARENT customers)
    // Ensures parent customer and child account are stored on the same split node
    // =========================================================================
    @Transactional
    public Customer createCustomerWithAccount(String fullName, String email, String accountType, BigDecimal initialBalance) {
        log.info("Creating customer {} with interleaved account type {}", fullName, accountType);

        String customerId = UUID.randomUUID().toString(); // Anti-Hotspot UUID
        Customer customer = Customer.builder()
                .customerId(customerId)
                .fullName(fullName)
                .email(email)
                .status("ACTIVE")
                .build();
        Customer savedCustomer = customerRepository.save(customer);

        Account account = Account.builder()
                .customerId(savedCustomer.getCustomerId()) // Parent FK matching composite primary key
                .accountId(UUID.randomUUID().toString())   // Anti-Hotspot UUID
                .accountType(accountType)
                .currency("USD")
                .balance(initialBalance)
                .build();
        accountRepository.save(account);

        return savedCustomer;
    }

    // =========================================================================
    // BEST PRACTICE 3: Application-Level Retry for Aborted Transactions (@Retryable)
    // Automatically retries transactions aborted by Spanner's lock manager during contention
    // =========================================================================
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    @Transactional
    public PaymentTransaction transferMoney(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        log.info("Executing atomic transfer of ${} from {} to {} with @Retryable enabled...",
                amount, sourceAccountId, targetAccountId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // BEST PRACTICE: SELECT FOR UPDATE acquires Exclusive Lock immediately, preventing lock upgrade aborts
        Account sourceAccount = accountRepository.findByAccountIdForUpdate(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountId));
        Account targetAccount = accountRepository.findByAccountIdForUpdate(targetAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + targetAccountId));

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance in source account: " + sourceAccountId);
        }

        // Debit source & Credit target
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // Record Audit Transaction
        PaymentTransaction txn = PaymentTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .sourceAccountId(sourceAccountId)
                .targetAccountId(targetAccountId)
                .amount(amount)
                .status("COMPLETED")
                .build();

        return paymentTransactionRepository.save(txn);
    }

    // =========================================================================
    // BEST PRACTICE 4: Multi-Table High-Performance JDBC Batching
    // Seeds Customers AND matching Interleaved Accounts in multi-table JDBC batches
    // =========================================================================
    @Transactional
    public List<Customer> seedBulkCustomers(int count) {
        log.info("Seeding {} customers AND matching interleaved bank accounts via JDBC batching (batch_size=50)...", count);
        List<Customer> customerList = new ArrayList<>();
        List<Account> accountList = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String customerId = UUID.randomUUID().toString(); // Anti-Hotspot UUID
            Customer customer = Customer.builder()
                    .customerId(customerId)
                    .fullName("Batch User " + i)
                    .email("user" + i + "@spannerbank.com")
                    .status("ACTIVE")
                    .build();
            customerList.add(customer);

            Account account = Account.builder()
                    .customerId(customerId)                   // Parent FK matching composite primary key
                    .accountId(UUID.randomUUID().toString())   // Anti-Hotspot UUID
                    .accountType("CHECKING")
                    .currency("USD")
                    .balance(BigDecimal.valueOf(1000.00))
                    .build();
            accountList.add(account);
        }

        // Save in multi-table JDBC batches
        List<Customer> savedCustomers = customerRepository.saveAll(customerList);
        accountRepository.saveAll(accountList);

        log.info("Successfully seeded {} customers and {} interleaved bank accounts!", savedCustomers.size(), accountList.size());
        return savedCustomers;
    }

    // =========================================================================
    // BEST PRACTICE 5: Partitioned DML for Massive Bulk Maintenance
    // Executes bulk DML without hitting Spanner's 80,000 mutation limit per transaction
    // =========================================================================
    @Transactional
    public int bulkUpdateAccountTypes(String oldType, String newType) {
        log.info("Executing Partitioned DML bulk update from account type {} to {}", oldType, newType);
        return accountRepository.bulkUpdateAccountTypePartitioned(oldType, newType);
    }
}
