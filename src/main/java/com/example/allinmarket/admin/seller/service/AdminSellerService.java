package com.example.allinmarket.admin.seller.service;

import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSellerService {
    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;

    public PageResponse<SellerDetailResponse> findAllSeller(Long currentUserId, Pageable pageable, SellerStatus status) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(currentUserId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }

        if (status == null) {
            return PageResponse.register(
                    sellerRepository.findAllByDeletedAtIsNull(pageable)
                            .map(SellerDetailResponse::from)
            );
        }

        return PageResponse.register(
                sellerRepository.findAllByStatusAndDeletedAtIsNull(status, pageable)
                        .map(SellerDetailResponse::from)
        );
    }
}
