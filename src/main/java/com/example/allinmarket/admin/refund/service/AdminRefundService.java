package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRefundService {

    private final RefundRepository refundRepository;

}
