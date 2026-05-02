package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Page<Refund> findAllBy(Pageable pageable);

    Optional<Refund> findByIdAndStatus(Long refundId, RefundStatus status);

    @Query("SELECT r FROM Refund r JOIN FETCH r.payment WHERE r.id = :refundId")
    Optional<Refund> findByIdWithPayment(@Param("refundId") Long refundId);

    Optional<Refund> findByPayment(Payment payment);

    @Query("""
        SELECT r
        FROM Refund r
        JOIN FETCH r.payment p
        JOIN FETCH p.order
        WHERE r.id = :refundId
    """)
    Optional<Refund> findByIdWithPaymentAndOrder(@Param("refundId") Long refundId);
}
