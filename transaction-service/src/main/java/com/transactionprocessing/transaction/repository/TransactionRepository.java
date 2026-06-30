package com.transactionprocessing.transaction.repository;

import com.transactionprocessing.transaction.entity.Transaction;
import com.transactionprocessing.transaction.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionIdAndRequestId(String transactionId, String requestId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    List<Transaction> findByStatusInOrderByAccountIdAscSequenceNumberAsc(List<TransactionStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM Transaction t
            WHERE t.accountId = :accountId
              AND t.sequenceNumber = :sequenceNumber
              AND t.status = com.transactionprocessing.transaction.entity.TransactionStatus.PROCESSED
            """)
    boolean isSequenceProcessed(@Param("accountId") String accountId,
                                @Param("sequenceNumber") int sequenceNumber);

    List<Transaction> findByAccountIdAndStatusOrderBySequenceNumberAsc(String accountId, TransactionStatus status);

    List<Transaction> findByStatus(TransactionStatus status);

    long countByStatus(TransactionStatus status);
}
