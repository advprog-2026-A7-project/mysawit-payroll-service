package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
