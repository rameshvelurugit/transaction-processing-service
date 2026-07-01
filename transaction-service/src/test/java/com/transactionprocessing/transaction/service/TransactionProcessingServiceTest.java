package com.transactionprocessing.transaction.service;

import com.transactionprocessing.common.exception.RetryableException;
import com.transactionprocessing.transaction.entity.Account;
import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.AccountRepository;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import com.transactionprocessing.transaction.util.ProcessingSimulator;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for core processing logic including sequencing, balance updates, and retryable failure handling.
 */
@ExtendWith(MockitoExtension.class)
class TransactionProcessingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private DuplicateDetectionService duplicateDetectionService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RetryService retryService;
    @Mock
    private ProcessingSummaryService processingSummaryService;

    private TransactionProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new TransactionProcessingService(
                transactionRepository,
                accountRepository,
                duplicateDetectionService,
                idempotencyService,
                retryService,
                processingSummaryService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void marksPendingWhenPreviousSequenceNotProcessed() {
        Transaction txn = transaction(2);
        when(duplicateDetectionService.hasProcessedRecord("TXN-1", "REQ-1")).thenReturn(false);
        when(transactionRepository.isSequenceProcessed("ACC-1001", 1)).thenReturn(false);

        var outcome = processingService.processSingle(txn);
        assertThat(outcome).isEqualTo(TransactionProcessingService.ProcessingOutcome.PENDING);
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void processesCreditSuccessfully() {
        Transaction txn = transaction(1);
        when(duplicateDetectionService.hasProcessedRecord("TXN-1", "REQ-1")).thenReturn(false);
        when(accountRepository.findById("ACC-1001")).thenReturn(Optional.of(
                Account.builder().accountId("ACC-1001").balance(new BigDecimal("100")).build()));
        when(transactionRepository.findByAccountIdAndStatusOrderBySequenceNumberAsc(
                eq("ACC-1001"), eq(TransactionStatus.PENDING))).thenReturn(java.util.List.of());

        var outcome = processingService.processSingle(txn);
        assertThat(outcome).isEqualTo(TransactionProcessingService.ProcessingOutcome.PROCESSED);
        verify(idempotencyService).recordProcessed(txn);
    }

    @Test
    void detectsDuplicateDuringProcessing() {
        Transaction txn = transaction(1);
        when(duplicateDetectionService.hasProcessedRecord("TXN-1", "REQ-1")).thenReturn(true);
        when(duplicateDetectionService.markAsDuplicate(txn)).thenReturn(txn);

        var outcome = processingService.processSingle(txn);
        assertThat(outcome).isEqualTo(TransactionProcessingService.ProcessingOutcome.DUPLICATE);
    }

    @Test
    void triggersRetryOnTransientFailure() {
        Transaction txn = transaction(1);
        txn.setAmount(ProcessingSimulator.TRANSIENT_FAILURE_AMOUNT);
        when(duplicateDetectionService.hasProcessedRecord("TXN-1", "REQ-1")).thenReturn(false);

        processingService.processSingle(txn);
        verify(retryService).handleRetryableFailure(eq(txn), any(RetryableException.class));
    }

    private Transaction transaction(int sequence) {
        return Transaction.builder()
                .id(1L)
                .transactionId("TXN-1")
                .requestId("REQ-1")
                .sequenceNumber(sequence)
                .accountId("ACC-1001")
                .amount(new BigDecimal("10.00"))
                .type("CREDIT")
                .status(TransactionStatus.RECEIVED)
                .retryCount(0)
                .build();
    }
}
