package com.transactionprocessing.transaction.service;

import com.transactionprocessing.transaction.entity.TransactionStatus;
import com.transactionprocessing.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingSummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private ProcessingSummaryService summaryService;

    @BeforeEach
    void setUp() {
        summaryService = new ProcessingSummaryService(transactionRepository);
    }

    @Test
    void aggregatesStatusCounts() {
        when(transactionRepository.countByStatus(TransactionStatus.RECEIVED)).thenReturn(2L);
        when(transactionRepository.countByStatus(TransactionStatus.PROCESSED)).thenReturn(5L);
        when(transactionRepository.countByStatus(TransactionStatus.FAILED)).thenReturn(1L);
        when(transactionRepository.countByStatus(TransactionStatus.PROCESSING)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(1L);
        when(transactionRepository.countByStatus(TransactionStatus.DUPLICATE)).thenReturn(1L);
        when(transactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(2L);

        var summary = summaryService.getSummary();
        assertThat(summary.getReceived()).isEqualTo(2);
        assertThat(summary.getProcessed()).isEqualTo(5);
        assertThat(summary.getFailed()).isEqualTo(1);
        assertThat(summary.getTotal()).isEqualTo(12);
    }
}
