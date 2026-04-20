package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private OrderItem createOrderItemMock(Seller seller, BigDecimal unitPrice, int quantity) {
        OrderItem item = mock(OrderItem.class);
        given(item.getSeller()).willReturn(seller);
        given(item.getUnitPrice()).willReturn(unitPrice);
        given(item.getQuantity()).willReturn(quantity);
        return item;
    }

    // ==================== 기존 대시보드 있을 때 ====================

    @Test
    void 기존_대시보드_있을때_cancelOrder_올바른_인자로_호출_테스트() {
        Long orderId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(10L);

        OrderItem item = createOrderItemMock(seller, BigDecimal.valueOf(10000), 2);
        given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(List.of(item));

        SellerDashboard dashboard = mock(SellerDashboard.class);
        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(10L), any(LocalDate.class)))
                .willReturn(Optional.of(dashboard));

        dashboardService.updateSellerDashboardWithRefund(orderId);

        // refundAmount = 10000 * 2 = 20000, productsRefunded = 2
        verify(dashboard).cancelOrder(BigDecimal.valueOf(20000), 2);
        verify(sellerDashboardRepository).save(dashboard);
    }

    // ==================== 대시보드 없을 때 ====================

    @Test
    void 대시보드_없을때_신규_생성_후_cancelOrder_반영_테스트() {
        Long orderId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(10L);

        OrderItem item = createOrderItemMock(seller, BigDecimal.valueOf(5000), 3);
        given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(List.of(item));

        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(10L), any(LocalDate.class)))
                .willReturn(Optional.empty());

        ArgumentCaptor<SellerDashboard> captor = ArgumentCaptor.forClass(SellerDashboard.class);
        dashboardService.updateSellerDashboardWithRefund(orderId);

        verify(sellerDashboardRepository).save(captor.capture());
        SellerDashboard saved = captor.getValue();
        // 5000 * 3 = 15000 이 refundAmount에 반영되어야 함
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(saved.getRefundAmount()));
        assertEquals(1, saved.getTotalRefunds());
    }

    // ==================== 다중 판매자 ====================

    @Test
    void 다중_판매자_각각_별도_cancelOrder_호출_테스트() {
        Long orderId = 1L;

        Seller sellerA = mock(Seller.class);
        given(sellerA.getId()).willReturn(1L);

        Seller sellerB = mock(Seller.class);
        given(sellerB.getId()).willReturn(2L);

        OrderItem itemA = createOrderItemMock(sellerA, BigDecimal.valueOf(3000), 1);
        OrderItem itemB = createOrderItemMock(sellerB, BigDecimal.valueOf(7000), 2);

        given(orderItemRepository.findAllByOrderIdWithSeller(orderId))
                .willReturn(List.of(itemA, itemB));

        SellerDashboard dashA = mock(SellerDashboard.class);
        SellerDashboard dashB = mock(SellerDashboard.class);
        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(1L), any())).willReturn(Optional.of(dashA));
        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(2L), any())).willReturn(Optional.of(dashB));

        dashboardService.updateSellerDashboardWithRefund(orderId);

        verify(dashA).cancelOrder(BigDecimal.valueOf(3000), 1);
        verify(dashB).cancelOrder(BigDecimal.valueOf(14000), 2);
        verify(sellerDashboardRepository, times(2)).save(any(SellerDashboard.class));
    }

    // ==================== 단일 판매자 + 다중 아이템 ====================

    @Test
    void 단일_판매자_다중_아이템_금액_합산_테스트() {
        Long orderId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(10L);

        // 아이템 3개: 1000*2 + 2000*1 + 500*4 = 2000 + 2000 + 2000 = 6000, 수량 합 = 7
        OrderItem item1 = createOrderItemMock(seller, BigDecimal.valueOf(1000), 2);
        OrderItem item2 = createOrderItemMock(seller, BigDecimal.valueOf(2000), 1);
        OrderItem item3 = createOrderItemMock(seller, BigDecimal.valueOf(500), 4);

        given(orderItemRepository.findAllByOrderIdWithSeller(orderId))
                .willReturn(List.of(item1, item2, item3));

        SellerDashboard dashboard = mock(SellerDashboard.class);
        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(10L), any())).willReturn(Optional.of(dashboard));

        dashboardService.updateSellerDashboardWithRefund(orderId);

        verify(dashboard).cancelOrder(BigDecimal.valueOf(6000), 7);
    }

    // ==================== OrderItem 없을 때 ====================

    @Test
    void OrderItem_없을때_save_미호출_테스트() {
        given(orderItemRepository.findAllByOrderIdWithSeller(1L)).willReturn(List.of());

        dashboardService.updateSellerDashboardWithRefund(1L);

        verify(sellerDashboardRepository, never()).save(any());
    }
}
