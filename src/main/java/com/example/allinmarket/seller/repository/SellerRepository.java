package com.example.allinmarket.seller.repository;

import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByEmail(String email);
    Optional<Seller> findByEmailAndDeletedAtIsNull(String email);

    Page<Seller> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Seller> findAllByStatusAndDeletedAtIsNull(SellerStatus status, Pageable pageable);
}
