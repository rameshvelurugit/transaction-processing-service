package com.transactionprocessing.transaction.service;

import com.transactionprocessing.transaction.entity.ProcessedRequest;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.repository.ProcessedRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks processed transaction/request ID pairs to guarantee idempotent, exactly-once processing semantics.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedRequestRepository processedRequestRepository;

    public boolean isAlreadyProcessed(String transactionId, String requestId) {
        return processedRequestRepository.existsByTransactionIdAndRequestId(transactionId, requestId);
    }

    @Transactional
    public void recordProcessed(Transaction transaction) {
        if (processedRequestRepository.existsByTransactionIdAndRequestId(
                transaction.getTransactionId(), transaction.getRequestId())) {
            return;
        }
        ProcessedRequest record = ProcessedRequest.builder()
                .transactionId(transaction.getTransactionId())
                .requestId(transaction.getRequestId())
                .internalTransactionId(transaction.getId())
                .build();
        processedRequestRepository.save(record);
    }
}
