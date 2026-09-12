package com.pmrgsolution.features.payment.scheduler;

import com.pmrgsolution.core.service.FileStorageService;
import com.pmrgsolution.features.payment.entity.Transaction;
import com.pmrgsolution.features.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduled job that automatically purges heavy payment proof screenshot images
 * older than 30 days from cloud/disk storage to conserve storage space.
 *
 * NOTE: The Order record, UTR Number, Payment status, and Financial Ledger remain 100% intact.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProofCleanupScheduler {

    private final TransactionRepository transactionRepository;
    private final FileStorageService fileStorageService;

    @Scheduled(cron = "0 0 3 * * ?") // Runs daily at 3:00 AM
    @Transactional
    public void cleanupExpiredPaymentProofs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Starting automated 30-day payment proof image cleanup for records before: {}", cutoff);

        List<Transaction> expiredTransactions = transactionRepository.findByCreatedAtBeforeAndPaymentProofUrlIsNotNull(cutoff);
        if (expiredTransactions.isEmpty()) {
            log.info("No expired payment proof images found for cleanup.");
            return;
        }

        int deletedCount = 0;
        for (Transaction tx : expiredTransactions) {
            String proofUrl = tx.getPaymentProofUrl();
            if (proofUrl != null && !proofUrl.isBlank()) {
                try {
                    fileStorageService.deleteFile(proofUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete payment proof file '{}' from storage: {}", proofUrl, e.getMessage());
                }
            }

            tx.setPaymentProofUrl(null);
            tx.setPaymentProof(null);
            tx.setPaymentProofContentType(null);
            deletedCount++;
        }

        transactionRepository.saveAll(expiredTransactions);
        log.info("Successfully purged {} expired payment proof screenshot(s) older than 30 days. UTR and Order records preserved.", deletedCount);
    }
}
