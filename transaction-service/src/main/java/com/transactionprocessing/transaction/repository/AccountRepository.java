package com.transactionprocessing.transaction.repository;

import com.transactionprocessing.transaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
