package com.example.allinmarket.admin.refund.controller;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.refund.service.AdminRefundService;
import com.example.allinmarket.common.config.TestRedisConfig;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminRefundController HTTP 레이어 테스트
 * <p>
 * 재시도(@Retryable) 로직은 서비스 레이어에 위치하므로 AdminRefundOptimisticLockServiceTest에서 검증한다.
 * 이 테스트는 컨트롤러가 담당하는 HTTP 관심사만 검증한다:
 * - 요청 URL/메서드 매핑
 * - 서비스 성공 시 200 OK + 응답 JSON 구조
 * - 서비스 예외 발생 시 5xx 응답
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class AdminRefundOptimisticLockControllerTest {

    private static final UsernamePasswordAuthenticationToken ADMIN_AUTH =
            new UsernamePasswordAuthenticationToken(
                    1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminRefundService adminRefundService;

    private AuthorizeRefundResponse successResponse(RefundStatus status) {
        return new AuthorizeRefundResponse(
                1L, 1L, 1L,
                ReasonEnum.CHANGE_OF_MIND,
                "환불 사유", null,
                status, null
        );
    }

    // ==================== complete ====================

    @Test
    void complete_서비스_성공시_200OK_반환() throws Exception {
        when(adminRefundService.complete(any(), any()))
                .thenReturn(successResponse(RefundStatus.SUCCESS));

        mockMvc.perform(put("/admin/refunds/1/complete")
                        .with(authentication(ADMIN_AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        verify(adminRefundService, times(1)).complete(any(), any());
    }

    @Test
    void complete_서비스_예외시_5xx_반환() throws Exception {
        when(adminRefundService.complete(any(), any()))
                .thenThrow(new OptimisticLockingFailureException("락 충돌"));

        mockMvc.perform(put("/admin/refunds/1/complete")
                        .with(authentication(ADMIN_AUTH)))
                .andExpect(status().is5xxServerError());

        verify(adminRefundService, times(1)).complete(any(), any());
    }

    // ==================== deny ====================

    @Test
    void deny_서비스_성공시_200OK_반환() throws Exception {
        when(adminRefundService.deny(any(), any(), any(DenyRefundRequest.class)))
                .thenReturn(successResponse(RefundStatus.DENIED));

        mockMvc.perform(put("/admin/refunds/1/deny")
                        .with(authentication(ADMIN_AUTH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deniedReason": "환불 불가 사유"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DENIED"));

        verify(adminRefundService, times(1)).deny(any(), any(), any(DenyRefundRequest.class));
    }

    @Test
    void deny_서비스_예외시_5xx_반환() throws Exception {
        when(adminRefundService.deny(any(), any(), any(DenyRefundRequest.class)))
                .thenThrow(new OptimisticLockingFailureException("락 충돌"));

        mockMvc.perform(put("/admin/refunds/1/deny")
                        .with(authentication(ADMIN_AUTH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deniedReason": "환불 불가 사유"}
                                """))
                .andExpect(status().is5xxServerError());

        verify(adminRefundService, times(1)).deny(any(), any(), any(DenyRefundRequest.class));
    }
}
