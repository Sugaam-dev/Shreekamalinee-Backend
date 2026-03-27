package com.PMRGSolution.RENAISSANCE.features.payment.repository;

import com.PMRGSolution.RENAISSANCE.features.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    // Used to find the pending record during verification
    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);

    // NEW: Critical for Idempotency
    // Prevents double-activation of subscriptions if the verify endpoint is hit twice
    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}