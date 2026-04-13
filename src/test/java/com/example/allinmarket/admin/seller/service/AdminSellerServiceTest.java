package com.example.allinmarket.admin.seller.service;

import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AdminSellerServiceTest {
    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminSellerService adminSellerService;

    @Test
    void 판매자_목록_조회_성공_테스트() {
        //given
        Admin admin = Admin.of(
                "관리자@테스트.com",
                "12345678",
                "관리자"
        );

        ReflectionTestUtils.setField(admin, "id", 1L);

        Seller seller = Seller.of(
                "판매자@테스트.com",
                "12345678",
                "판매자",
                "010-1234-5678",
                "상점 이름",
                "사업자 번호",
                "계좌 정보"
        );

        ReflectionTestUtils.setField(seller, "id", 1L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller), pageable, 1);

        given(adminRepository.existsByIdAndDeletedAtIsNull(admin.getId())).willReturn(true);
        given(sellerRepository.findAllByDeletedAtIsNull(pageable)).willReturn(sellerPage);

        // when
        PageResponse<SellerDetailResponse> responses = adminSellerService.findAllSeller(admin.getId(), pageable, null);

        // then
        assertEquals(1, responses.totalElements());
        assertEquals("판매자", responses.content().get(0).name());
    }

    @Test
    void 판매자_목록_조회_실패_테스트() {
        // given
        given(adminRepository.existsByIdAndDeletedAtIsNull(1L)).willReturn(true);

        given(sellerRepository.findAllByDeletedAtIsNull(any(Pageable.class))).willThrow(
                new BaseException(ErrorEnum.SELLER_NOT_FOUND)
        );

        // when & then
        assertThrows(BaseException.class, () -> adminSellerService.findAllSeller(1L, PageRequest.of(0, 10), null));
    }
}
