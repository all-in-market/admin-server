package com.example.allinmarket.admin.refund.controller;

import com.example.allinmarket.admin.refund.service.AdminRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;


}
