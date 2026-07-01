package com.transactionprocessing.transaction.repository;

import com.transactionprocessing.transaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for persisting and retrieving account balance records.
 */
public interface AccountRepository extends JpaRepository<Account, String> {
}
