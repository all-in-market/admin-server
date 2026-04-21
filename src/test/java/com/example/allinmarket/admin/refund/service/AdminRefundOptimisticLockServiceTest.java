package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AdminRefundOptimisticLockServiceTest {

    @Autowired
    private AdminRefundService adminRefundService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long adminId;
    private Long refundId;

    @BeforeEach
    void setUp() {
        Admin admin = adminRepository.save(Admin.of("admin@test.com", "password", "관리자"));
        adminId = admin.getId();

        refundId = transactionTemplate.execute(status -> {
            Buyer buyer = buyerRepository.save(
                    Buyer.of("buyer@test.com", "password", "구매자", "010-1234-5678")
            );
            Order order = orderRepository.save(
                    Order.of(buyer, BigDecimal.valueOf(10000), null, "수령인", "010-1234-5678", "서울시")
            );
            order.updateStatus(OrderStatus.PAID);

            Payment payment = paymentRepository.save(
                    Payment.of(order, "mock-imp-uid", BigDecimal.valueOf(10000), MethodEnum.MOCK)
            );

            Refund refund = refundRepository.save(
                    Refund.of(buyer, payment, ReasonEnum.CHANGE_OF_MIND, "환불 사유")
            );
            return refund.getId();
        });
    }

    @AfterEach
    void tearDown() {
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        buyerRepository.deleteAll();
        adminRepository.deleteAll();
    }

    // ==================== complete - 낙관적 락 충돌 테스트 ====================

    /**
     * 두 영속성 컨텍스트가 동일한 Refund를 동시에 읽은 뒤,
     * 첫 번째가 커밋하면 version이 증가한다.
     * 두 번째가 stale version으로 커밋을 시도하면 OptimisticLockException이 발생해야 한다.
     */
    @Test
    void complete_두_영속성컨텍스트_충돌시_OptimisticLockException_발생() {
        // given: EntityManager 1 이 Refund를 읽어 둠 (version=0)
        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        Refund refundInEm1 = em1.find(Refund.class, refundId);

        // EntityManager 2 도 같은 Refund를 독립적으로 읽어 둠 (version=0)
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        Refund refundInEm2 = em2.find(Refund.class, refundId);

        // when: EntityManager 1 이 먼저 complete 후 커밋 → version이 1이 됨
        refundInEm1.complete();
        refundInEm1.getPayment().cancel();
        refundInEm1.getPayment().getOrder().updateStatus(OrderStatus.REFUNDED);
        em1.getTransaction().commit();
        em1.close();

        // then: EntityManager 2 가 stale version(0)으로 커밋 시도 → 예외 발생
        refundInEm2.complete();
        assertThatThrownBy(() -> em2.getTransaction().commit())
                .isInstanceOf(RollbackException.class)
                .hasRootCauseInstanceOf(StaleStateException.class);
        em2.close();
    }

    /**
     * 동일한 Refund에 대해 두 스레드가 동시에 complete()를 호출할 때
     * 정확히 하나만 성공하고 나머지는 OptimisticLockingFailureException이 발생해야 한다.
     */
    @Test
    void complete_동시_요청시_하나만_성공하고_나머지는_예외발생() throws InterruptedException {
        // given
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 두 스레드가 동시에 complete() 호출
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시에 출발
                    adminRefundService.complete(adminId, refundId);
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // status 불일치로 REFUND_NOT_FOUND가 발생할 수도 있음 (두 번째 스레드가 늦게 읽은 경우)
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시에 시작
        doneLatch.await();
        executor.shutdown();

        // then: 정확히 하나만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }

    // ==================== deny - 낙관적 락 충돌 테스트 ====================

    /**
     * 두 영속성 컨텍스트가 동일한 Refund를 동시에 읽은 뒤,
     * 첫 번째가 deny 후 커밋하면 version이 증가한다.
     * 두 번째가 stale version으로 커밋을 시도하면 OptimisticLockException이 발생해야 한다.
     */
    @Test
    void deny_두_영속성컨텍스트_충돌시_OptimisticLockException_발생() {
        // given: EntityManager 1 이 Refund를 읽어 둠 (version=0)
        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        Refund refundInEm1 = em1.find(Refund.class, refundId);

        // EntityManager 2 도 같은 Refund를 독립적으로 읽어 둠 (version=0)
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        Refund refundInEm2 = em2.find(Refund.class, refundId);

        // when: EntityManager 1 이 먼저 deny 후 커밋 → version이 1이 됨
        refundInEm1.deny("첫 번째 거절 사유");
        em1.getTransaction().commit();
        em1.close();

        // then: EntityManager 2 가 stale version(0)으로 커밋 시도 → 예외 발생
        refundInEm2.deny("두 번째 거절 시도");
        assertThatThrownBy(() -> em2.getTransaction().commit())
                .isInstanceOf(RollbackException.class)
                .hasRootCauseInstanceOf(StaleStateException.class);
        em2.close();
    }

    /**
     * 동일한 Refund에 대해 두 스레드가 동시에 deny()를 호출할 때
     * 정확히 하나만 성공하고 나머지는 OptimisticLockingFailureException이 발생해야 한다.
     */
    @Test
    void deny_동시_요청시_하나만_성공하고_나머지는_예외발생() throws InterruptedException {
        // given
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        DenyRefundRequest request = new DenyRefundRequest("환불 거절 사유");

        // when: 두 스레드가 동시에 deny() 호출
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시에 출발
                    adminRefundService.deny(adminId, refundId, request);
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then: 정확히 하나만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    // ==================== version 필드 변화 검증 ====================

    @Test
    void complete_호출_후_version_증가_확인() {
        // given
        Refund before = refundRepository.findById(refundId).orElseThrow();
        Long versionBefore = before.getVersion();

        // when
        adminRefundService.complete(adminId, refundId);

        // then
        EntityManager em = emf.createEntityManager();
        Refund after = em.find(Refund.class, refundId);
        em.close();

        assertThat(after.getVersion()).isGreaterThan(versionBefore);
        assertThat(after.getStatus()).isEqualTo(RefundStatus.SUCCESS);
    }

    @Test
    void deny_호출_후_version_증가_확인() {
        // given
        Refund before = refundRepository.findById(refundId).orElseThrow();
        Long versionBefore = before.getVersion();

        // when
        adminRefundService.deny(adminId, refundId, new DenyRefundRequest("거절 사유"));

        // then
        EntityManager em = emf.createEntityManager();
        Refund after = em.find(Refund.class, refundId);
        em.close();

        assertThat(after.getVersion()).isGreaterThan(versionBefore);
        assertThat(after.getStatus()).isEqualTo(RefundStatus.DENIED);
    }

    @Test
    void 조회만_할_경우_version_변경없음_확인() {
        // given
        Refund before = refundRepository.findById(refundId).orElseThrow();
        Long versionBefore = before.getVersion();

        // when: 단순 조회 (findAll)
        adminRefundService.findAll(adminId, org.springframework.data.domain.PageRequest.of(0, 10));

        // then
        Refund after = refundRepository.findById(refundId).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(versionBefore);
    }

    /**
     * Refund 엔티티에 @Version 필드가 선언되어 있고,
     * 초기 저장 시 version 값이 null이 아닌지 확인한다.
     */
    @Test
    void Refund_저장시_version_초기값_null이_아님() {
        Refund refund = refundRepository.findById(refundId).orElseThrow();
        assertTrue(refund.getVersion() != null, "@Version 필드는 저장 후 null이면 안 됩니다");
    }
}
