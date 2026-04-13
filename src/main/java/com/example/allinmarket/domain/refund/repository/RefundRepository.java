package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Page<Refund> findAllBy(Pageable pageable);

    @Query("""
    SELECT r FROM Refund r
    WHERE r.id = :refundId
    AND r.status = TransactionStatus.PENDING
    """)
    Optional<Refund> findByIdAndStatusPending(Long refundId);
}
