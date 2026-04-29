package com.example.allinmarket.admin.auth.dto.response;

import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;

public record LoginResult(BuyerLoginResponse response, String refreshToken) {
}