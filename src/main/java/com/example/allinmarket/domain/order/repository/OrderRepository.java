package com.example.allinmarket.domain.order.repository;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndBuyerId(Long orderId, Long buyerId);

    Page<Order> findAllBy(Pageable pageable);

    @Query("""
            select o from Order o join fetch o.buyer b
            where o.id = :orderId
            """)
    Optional<Order> findByIdWithBuyer(Long orderId);
}
