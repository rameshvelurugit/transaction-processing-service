package com.transactionprocessing.transaction.config;

import com.transactionprocessing.transaction.repository.TransactionRepository;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator health indicator reporting transaction backlog and failure counts, degrading when failures exceed a threshold.
 */
@Component
public class TransactionHealthIndicator implements HealthIndicator {

    private final TransactionRepository transactionRepository;

    public TransactionHealthIndicator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Health health() {
        long failed = transactionRepository.countByStatus(TransactionStatus.FAILED);
        long retryPending = transactionRepository.countByStatus(TransactionStatus.RETRY_PENDING);
        long received = transactionRepository.countByStatus(TransactionStatus.RECEIVED);

        Health.Builder builder = Health.up()
                .withDetail("failedTransactions", failed)
                .withDetail("retryPending", retryPending)
                .withDetail("receivedBacklog", received);

        if (failed > 10) {
            builder.status("DEGRADED").withDetail("reason", "High failed transaction count");
        }
        return builder.build();
    }
}
