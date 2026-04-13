package com.example.allinmarket.admin.order.service;

import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public PageResponse<OrderDetailResponse> findAll(Pageable pageable) {
        return PageResponse.register(
                orderRepository.findAllBy(pageable)
                        .map(OrderDetailResponse::from)
        );
    }
}
