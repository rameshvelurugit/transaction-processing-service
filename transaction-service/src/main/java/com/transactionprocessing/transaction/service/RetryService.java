package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.constants.TransactionConstants;
import com.transactionprocessing.common.exception.RetryableException;
import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages retry attempts and permanent failure marking when transaction processing encounters errors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void handleRetryableFailure(Transaction transaction, RetryableException exception) {
        MdcContext.setTransactionId(transaction.getTransactionId());
        log.warn("Retry started for transaction id={} attempt={}", transaction.getId(), transaction.getRetryCount() + 1);

        if (transaction.getRetryCount() >= TransactionConstants.MAX_RETRY_ATTEMPTS) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage("Max retry attempts exceeded: " + exception.getMessage());
            transactionRepository.save(transaction);
            meterRegistry.counter("transactions.failed", "reason", "max_retries").increment();
            log.error("Retry completed with permanent failure for transaction id={}", transaction.getId());
            return;
        }

        transaction.setRetryCount(transaction.getRetryCount() + 1);
        transaction.setStatus(TransactionStatus.RETRY_PENDING);
        transaction.setErrorMessage(exception.getMessage());
        transactionRepository.save(transaction);
        meterRegistry.counter("transactions.retry", "attempt", String.valueOf(transaction.getRetryCount())).increment();
        log.info("Retry completed - transaction id={} marked RETRY_PENDING", transaction.getId());
    }

    @Transactional
    public void markPermanentFailure(Transaction transaction, String message) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setErrorMessage(message);
        transactionRepository.save(transaction);
        meterRegistry.counter("transactions.failed", "reason", "permanent").increment();
    }
}
