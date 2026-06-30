package com.transactionprocessing.transaction.service;

import com.transactionprocessing.transaction.entity.ProcessedRequest;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.repository.ProcessedRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedRequestRepository processedRequestRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void doesNotRecordDuplicateProcessedRequest() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .build();
        when(processedRequestRepository.existsByTransactionIdAndRequestId("TXN-1", "REQ-1")).thenReturn(true);

        idempotencyService.recordProcessed(txn);

        verify(processedRequestRepository, never()).save(any(ProcessedRequest.class));
    }
}
