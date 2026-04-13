package com.example.allinmarket.admin.refund.dto.request;

import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;

public record AuthorizeRefundRequest (
        String deniedReason,
        TransactionStatus status
){}
