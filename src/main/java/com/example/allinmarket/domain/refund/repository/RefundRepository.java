package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Page<Refund> findAllBy(Pageable pageable);
}
