package com.example.allinmarket.admin.seller.controller;

import com.example.allinmarket.admin.seller.service.AdminSellerService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AdminSellerController.class)
@AutoConfigureRestTestClient
public class AdminSellerControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminSellerService adminSellerService;

    @BeforeEach
    void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 판매자_목록_조회_성공_테스트() {
        // given
        SellerDetailResponse response = new SellerDetailResponse(
                1L,
                "판매자@테스트.com",
                "판매자",
                "010-1234-1234",
                "상점 이름",
                "사업자 번호",
                "계좌 정보",
                SellerStatus.APPROVED,
                UserRole.SELLER
        );

        Page<SellerDetailResponse> page = new PageImpl<>(List.of(response));

        PageResponse<SellerDetailResponse> pageResponse = PageResponse.register(page);

        given(adminSellerService.findAllSeller(eq(1L), any(Pageable.class), eq(SellerStatus.APPROVED)))
                .willReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/admin/sellers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.READ_SUCCESS.getMessage());
    }

    @Test
    @WithMockUser
    void 판매자_목록_조회_실패_테스트() {
        // given
        given(adminSellerService.findAllSeller(any(), any(), any()))
                .willThrow(new BaseException(ErrorEnum.UNAUTHORIZED));

        // when & then
        restTestClient.get().uri("/admin/sellers")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo(ErrorEnum.UNAUTHORIZED.getMessage());
    }
}
