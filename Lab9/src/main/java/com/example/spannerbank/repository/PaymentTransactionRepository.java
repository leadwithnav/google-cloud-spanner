package com.example.spannerbank.repository;

import com.example.spannerbank.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    List<PaymentTransaction> findBySourceAccountId(String sourceAccountId);
}
