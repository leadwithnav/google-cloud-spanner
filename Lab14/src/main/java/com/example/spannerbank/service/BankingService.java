package com.example.spannerbank.service;

import com.example.spannerbank.model.Account;
import com.example.spannerbank.model.Customer;
import com.example.spannerbank.model.PaymentTransaction;
import com.example.spannerbank.repository.AccountRepository;
import com.example.spannerbank.repository.CustomerRepository;
import com.example.spannerbank.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // BEST PRACTICE 1: Use @Transactional(readOnly = true) for Read Operations
    // In Spanner + PGAdapter, read-only transactions use Snapshot Reads which
    // do NOT acquire locks and avoid lock contention with concurrent writes.
    // =========================================================================
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        log.info("Fetching all customers using Spanner Snapshot Read...");
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
    }

    @Transactional(readOnly = true)
    public List<Account> getCustomerAccounts(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    // =========================================================================
    // BEST PRACTICE 2: Read-Write Transactions for Atomic Updates
    // Spanner uses serializable pessimistic locking for Read-Write transactions.
    // Ensure write transactions are short-lived and update only required rows.
    // =========================================================================
    @Transactional
    public Customer createCustomerWithAccount(String fullName, String email, String accountType, BigDecimal initialBalance) {
        log.info("Creating new Customer and Account in a single Read-Write transaction...");

        Customer customer = Customer.builder()
                .customerId(UUID.randomUUID().toString()) // Anti-Hotspot: UUID key
                .fullName(fullName)
                .email(email)
                .status("ACTIVE")
                .build();
        Customer savedCustomer = customerRepository.save(customer);

        Account account = Account.builder()
                .accountId(UUID.randomUUID().toString())   // Anti-Hotspot: UUID key
                .customerId(savedCustomer.getCustomerId())
                .accountType(accountType)
                .currency("USD")
                .balance(initialBalance)
                .build();
        accountRepository.save(account);

        return savedCustomer;
    }

    // =========================================================================
    // BEST PRACTICE 3: Money Transfer Atomic Transaction
    // Ensures atomic debit/credit updates across Spanner splits with serializable isolation.
    // =========================================================================
    @Transactional
    public PaymentTransaction transferMoney(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        log.info("Executing money transfer of ${} from account {} to {}", amount, sourceAccountId, targetAccountId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountId));
        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + targetAccountId));

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance in source account");
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
    // BEST PRACTICE 4: High-Performance Bulk Batch Insertion
    // Leverages Hibernate batching (hibernate.jdbc.batch_size=50) to bundle
    // multiple inserts into a single network RPC call to Spanner via PGAdapter.
    // =========================================================================
    @Transactional
    public List<Customer> seedBulkCustomers(int count) {
        log.info("Seeding {} customers AND matching bank accounts using Hibernate JDBC batching...", count);
        List<Customer> customerList = new ArrayList<>();
        List<Account> accountList = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String customerId = UUID.randomUUID().toString();
            Customer customer = Customer.builder()
                    .customerId(customerId)
                    .fullName("Batch User " + i)
                    .email("user" + i + "@spannerbank.com")
                    .status("ACTIVE")
                    .build();
            customerList.add(customer);

            Account account = Account.builder()
                    .accountId(UUID.randomUUID().toString())
                    .customerId(customerId)
                    .accountType("CHECKING")
                    .currency("USD")
                    .balance(BigDecimal.valueOf(1000.00))
                    .build();
            accountList.add(account);
        }

        // saveAll triggers JDBC batching (hibernate.jdbc.batch_size=50) for both tables
        List<Customer> savedCustomers = customerRepository.saveAll(customerList);
        accountRepository.saveAll(accountList);

        log.info("Successfully created {} customers and {} bank accounts in Spanner!", savedCustomers.size(), accountList.size());
        return savedCustomers;
    }
}
