package com.transactionprocessing.transaction.repository;

import com.transactionprocessing.transaction.entity.ProcessedRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, Long> {

    boolean existsByTransactionIdAndRequestId(String transactionId, String requestId);

    Optional<ProcessedRequest> findByTransactionIdAndRequestId(String transactionId, String requestId);
}
