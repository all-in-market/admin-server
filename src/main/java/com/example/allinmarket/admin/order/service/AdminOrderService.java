package com.example.allinmarket.admin.order.service;

import com.example.allinmarket.admin.order.dto.request.AdminOrderUpdateStatusRequest;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
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
    private final AdminRepository adminRepository;

    public PageResponse<OrderDetailResponse> findAll(Long adminId, Pageable pageable) {

        validationForbidden(adminId);

        return PageResponse.register(
                orderRepository.findAllBy(pageable)
                        .map(OrderDetailResponse::from)
        );
    }

    @Transactional
    public OrderDetailResponse updateStaus(Long adminId, Long orderId, AdminOrderUpdateStatusRequest request) {

        validationForbidden(adminId);

        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new BaseException(ErrorEnum.ORDER_NOT_FOUND)
        );

        order.updateStatus(request.status());

        return OrderDetailResponse.from(order);
    }

    private void validationForbidden(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }
}
