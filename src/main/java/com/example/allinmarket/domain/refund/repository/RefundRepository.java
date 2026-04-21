package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Page<Refund> findAllBy(Pageable pageable);

    Optional<Refund> findByIdAndStatus(Long refundId, RefundStatus status);
}
