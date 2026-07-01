package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.logging.MdcContext;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.ProcessedRequestRepository;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Detects duplicate transaction submissions at ingestion and processing time and marks them accordingly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final ProcessedRequestRepository processedRequestRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    public boolean isDuplicate(String transactionId, String requestId) {
        return idempotencyService.isAlreadyProcessed(transactionId, requestId);
    }

    public Optional<Transaction> findExistingTransaction(String transactionId, String requestId) {
        return transactionRepository.findByTransactionIdAndRequestId(transactionId, requestId);
    }

    @Transactional
    public Transaction markAsDuplicate(Transaction transaction) {
        MdcContext.setTransactionId(transaction.getTransactionId());
        MdcContext.setRequestId(transaction.getRequestId());
        log.info("Duplicate detected for transactionId={} requestId={}",
                transaction.getTransactionId(), transaction.getRequestId());
        transaction.setStatus(TransactionStatus.DUPLICATE);
        return transactionRepository.save(transaction);
    }

    public boolean hasProcessedRecord(String transactionId, String requestId) {
        return processedRequestRepository.existsByTransactionIdAndRequestId(transactionId, requestId);
    }
}
