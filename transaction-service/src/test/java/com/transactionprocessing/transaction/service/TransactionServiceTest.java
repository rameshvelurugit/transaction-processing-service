package com.transactionprocessing.transaction.service;

import com.transactionprocessing.transaction.dto.TransactionRequest;
import com.transactionprocessing.transaction.entity.Account;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.AccountRepository;
import com.transactionprocessing.transaction.repository.ProcessedRequestRepository;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for transaction ingestion, duplicate detection, and idempotent re-submission handling.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private com.transactionprocessing.transaction.mapper.TransactionMapper transactionMapper;
    @Mock
    private DuplicateDetectionService duplicateDetectionService;
    @Mock
    private TransactionProcessingService transactionProcessingService;
    @Mock
    private ProcessingSummaryService processingSummaryService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                transactionMapper,
                duplicateDetectionService,
                transactionProcessingService,
                processingSummaryService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void submitPersistsNewTransaction() {
        TransactionRequest request = validRequest("TXN-A", "REQ-A");
        when(duplicateDetectionService.isDuplicate("TXN-A", "REQ-A")).thenReturn(false);
        when(duplicateDetectionService.findExistingTransaction("TXN-A", "REQ-A")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(transactionMapper.toResponse(any())).thenReturn(
                com.transactionprocessing.transaction.dto.TransactionResponse.builder()
                        .id(1L)
                        .status(TransactionStatus.RECEIVED)
                        .build());

        var response = transactionService.submit(request);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.RECEIVED);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void submitMarksDuplicateWhenAlreadyProcessed() {
        TransactionRequest request = validRequest("TXN-DUP", "REQ-DUP");
        when(duplicateDetectionService.isDuplicate("TXN-DUP", "REQ-DUP")).thenReturn(true);
        Transaction existing = Transaction.builder()
                .id(9L)
                .transactionId("TXN-DUP")
                .requestId("REQ-DUP")
                .status(TransactionStatus.PROCESSED)
                .build();
        when(duplicateDetectionService.findExistingTransaction("TXN-DUP", "REQ-DUP"))
                .thenReturn(Optional.of(existing));
        when(duplicateDetectionService.markAsDuplicate(existing)).thenAnswer(inv -> {
            existing.setStatus(TransactionStatus.DUPLICATE);
            return existing;
        });
        when(transactionMapper.toResponse(any())).thenReturn(
                com.transactionprocessing.transaction.dto.TransactionResponse.builder()
                        .status(TransactionStatus.DUPLICATE)
                        .build());

        var response = transactionService.submit(request);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DUPLICATE);
    }

    private TransactionRequest validRequest(String txnId, String reqId) {
        return TransactionRequest.builder()
                .transactionId(txnId)
                .requestId(reqId)
                .sequenceNumber(1)
                .accountId("ACC-1001")
                .amount(new BigDecimal("25.00"))
                .type("CREDIT")
                .build();
    }
}
