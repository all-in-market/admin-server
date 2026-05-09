package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.refund.client.PortOnePaymentResponse;
import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.facade.AdminRefundFacade;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.config.TestRedisConfig;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
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
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Testcontainers
class AdminRefundOptimisticLockServiceTest {

    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    @Autowired
    private AdminRefundService adminRefundService;

    @Autowired
    private AdminRefundFacade adminRefundFacade;

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
    private String impUid;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-only");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @BeforeEach
    void setUp() {
        impUid = "mock-imp-uid-" + UUID.randomUUID();

        Admin admin = adminRepository.save(Admin.of("admin@test.com", "password", "관리자"));
        adminId = admin.getId();

        refundId = transactionTemplate.execute(status -> {
            Buyer buyer = buyerRepository.save(
                    Buyer.of("buyer@test.com", "password", "구매자", "010-1234-5678")
            );

            Order order = orderRepository.save(
                    Order.of(buyer, AMOUNT, null, "수령인", "010-1234-5678", "서울시")
            );
            order.updateStatus(OrderStatus.PAID);

            Payment payment = Payment.of(order, "mock-merchant-uid", AMOUNT, MethodEnum.MOCK);
            ReflectionTestUtils.setField(payment, "impUid", impUid);
            payment.success(LocalDateTime.now());
            paymentRepository.save(payment);

            Refund refund = refundRepository.save(
                    Refund.of(buyer, payment, ReasonEnum.CHANGE_OF_MIND, "환불 사유")
            );

            return refund.getId();
        });
    }

    @AfterEach
    void tearDown() {
        refundRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        buyerRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
    }

    @Test
    void startRefundProcessing_호출시_PENDING에서_PROCESSING으로_변경된다() {
        // when
        adminRefundService.startRefundProcessing(refundId);

        // then
        Refund refund = refundRepository.findById(refundId).orElseThrow();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
    }

    @Test
    void complete_두_영속성컨텍스트_충돌시_OptimisticLockException_발생() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        try {
            em1.getTransaction().begin();
            Refund refundInEm1 = em1.find(Refund.class, refundId);

            em2.getTransaction().begin();
            Refund refundInEm2 = em2.find(Refund.class, refundId);

            refundInEm1.processing();
            refundInEm1.success();
            refundInEm1.getPayment().cancel();
            refundInEm1.getPayment().getOrder().updateStatus(OrderStatus.REFUNDED);
            em1.getTransaction().commit();

            refundInEm2.processing();
            refundInEm2.success();

            assertThatThrownBy(() -> em2.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasRootCauseInstanceOf(StaleStateException.class);
        } finally {
            if (em1.isOpen()) {
                em1.close();
            }
            if (em2.isOpen()) {
                em2.close();
            }
        }
    }

    @Test
    void complete_동시_요청시_최종적으로_SUCCESS가_된다() throws InterruptedException {
        // given: complete()는 PROCESSING 상태만 처리 가능하다.
        adminRefundService.startRefundProcessing(refundId);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        PortOnePaymentResponse canceledPayment = cancelledPaymentResponse();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    adminRefundService.complete(refundId, canceledPayment);
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
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        Refund refund = refundRepository.findById(refundId).orElseThrow();
        Payment payment = paymentRepository.findById(refund.getPayment().getId()).orElseThrow();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void processRefund_동시_요청시_최종적으로_한번만_환불완료() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    adminRefundFacade.processRefund(adminId, refundId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        Refund refund = refundRepository.findById(refundId).orElseThrow();
        Payment payment = paymentRepository.findById(refund.getPayment().getId()).orElseThrow();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void deny_두_영속성컨텍스트_충돌시_OptimisticLockException_발생() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        try {
            em1.getTransaction().begin();
            Refund refundInEm1 = em1.find(Refund.class, refundId);

            em2.getTransaction().begin();
            Refund refundInEm2 = em2.find(Refund.class, refundId);

            refundInEm1.deny("첫 번째 거절 사유");
            em1.getTransaction().commit();

            refundInEm2.deny("두 번째 거절 시도");

            assertThatThrownBy(() -> em2.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasRootCauseInstanceOf(StaleStateException.class);
        } finally {
            if (em1.isOpen()) {
                em1.close();
            }
            if (em2.isOpen()) {
                em2.close();
            }
        }
    }

    @Test
    void deny_동시_요청시_하나만_성공하고_나머지는_예외발생() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        DenyRefundRequest request = new DenyRefundRequest("환불 거절 사유");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
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
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    @Test
    void complete_호출_후_version_증가_확인() {
        adminRefundService.startRefundProcessing(refundId);
        Long versionBefore = refundRepository.findById(refundId).orElseThrow().getVersion();

        adminRefundService.complete(refundId, cancelledPaymentResponse());

        EntityManager em = emf.createEntityManager();
        Refund after = em.find(Refund.class, refundId);
        em.close();

        assertThat(after.getVersion()).isGreaterThan(versionBefore);
        assertThat(after.getStatus()).isEqualTo(RefundStatus.SUCCESS);
    }

    @Test
    void processRefund_호출_후_환불과_결제상태가_변경됨() {
        Long versionBefore = refundRepository.findById(refundId).orElseThrow().getVersion();

        adminRefundFacade.processRefund(adminId, refundId);

        Refund refund = refundRepository.findById(refundId).orElseThrow();
        Payment payment = paymentRepository.findById(refund.getPayment().getId()).orElseThrow();

        assertThat(refund.getVersion()).isGreaterThan(versionBefore);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void deny_호출_후_version_증가_확인() {
        Long versionBefore = refundRepository.findById(refundId).orElseThrow().getVersion();

        adminRefundService.deny(adminId, refundId, new DenyRefundRequest("거절 사유"));

        EntityManager em = emf.createEntityManager();
        Refund after = em.find(Refund.class, refundId);
        em.close();

        assertThat(after.getVersion()).isGreaterThan(versionBefore);
        assertThat(after.getStatus()).isEqualTo(RefundStatus.DENIED);
    }

    @Test
    void 조회만_할_경우_version_변경없음_확인() {
        Long versionBefore = refundRepository.findById(refundId).orElseThrow().getVersion();

        adminRefundService.findAll(adminId, org.springframework.data.domain.PageRequest.of(0, 10));

        Refund after = refundRepository.findById(refundId).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(versionBefore);
    }

    @Test
    void Refund_저장시_version_초기값_null이_아님() {
        Refund refund = refundRepository.findById(refundId).orElseThrow();
        assertTrue(refund.getVersion() != null, "@Version 필드는 저장 후 null이면 안 됩니다");
    }

    private PortOnePaymentResponse cancelledPaymentResponse() {
        return paymentResponse("CANCELLED");
    }

    private PortOnePaymentResponse paymentResponse(String status) {
        return new PortOnePaymentResponse(
                status,
                impUid,
                "tx_" + impUid,
                "mock-merchant-uid",
                "store_mock",
                null,
                null,
                "v1",
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(System.currentTimeMillis()),
                "mock order",
                new PortOnePaymentResponse.Amount(
                        AMOUNT,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        AMOUNT,
                        AMOUNT,
                        BigDecimal.ZERO
                ),
                "KRW",
                null,
                String.valueOf(System.currentTimeMillis()),
                "pg_tx_mock"
        );
    }
}
