package com.example.spannerbank.repository;

import com.example.spannerbank.model.Account;
import com.example.spannerbank.model.AccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, AccountId> {

    // Derived query for fetching interleaved accounts belonging to a customer
    List<Account> findByCustomerId(String customerId);

    // Lookup single account by accountId
    Optional<Account> findByAccountId(String accountId);

    // BEST PRACTICE: SELECT FOR UPDATE acquires Exclusive Lock immediately to prevent lock upgrade aborts
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByAccountIdForUpdate(@Param("accountId") String accountId);

    // BEST PRACTICE: Partitioned DML query for massive bulk updates without 80,000 mutation limit
    @Modifying
    @Query("UPDATE Account a SET a.accountType = :newType WHERE a.accountType = :oldType")
    int bulkUpdateAccountTypePartitioned(@Param("oldType") String oldType, @Param("newType") String newType);
}
