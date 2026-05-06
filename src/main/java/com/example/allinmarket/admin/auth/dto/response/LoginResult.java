package com.example.allinmarket.admin.auth.dto.response;

import com.example.allinmarket.common.auth.dto.LoginResponse;

public record LoginResult(LoginResponse response, String refreshToken) {
}