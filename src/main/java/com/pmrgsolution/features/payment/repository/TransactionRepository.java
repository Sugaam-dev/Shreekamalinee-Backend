package com.pmrgsolution.features.payment.repository;

import com.pmrgsolution.features.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByOrderId(UUID orderId);
    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Transaction> findByUtrNumber(String utrNumber);
    List<Transaction> findByCreatedAtBeforeAndPaymentProofUrlIsNotNull(LocalDateTime cutoff);
    void deleteByOrderId(UUID orderId);
    void deleteByOrderIdIn(List<UUID> orderIds);
}