package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.ConcurrencyTestHelper;
import com.loopers.utils.ConcurrencyTestHelper.ConcurrencyResult;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    private static final int THREAD_COUNT = 100;

    private Long brandId;
    private Long userId;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        Brand brand = brandService.create("Nike", null);
        brandId = brand.getId();

        UserModel user = userJpaRepository.save(new UserModel(
            "concurrencyuser", "encoded", "동시성테스트유저", LocalDate.of(1990, 1, 1), "concurrency@test.com"
        ));
        userId = user.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동시에 같은 상품을 주문하면, 재고가 1개일 때 정확히 1건만 성공한다.")
    @Test
    void concurrentOrders_onlyOneSucceedsWhenStockIsOne() throws Exception {
        // arrange
        Product product = productService.create(brandId, "한정판 운동화", null, 1000, 1, SellingStatus.SELLING);
        Long productId = product.getId();

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(THREAD_COUNT, () -> orderApplicationService.create(userId, productId, 1, null));

        // assert
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(THREAD_COUNT - 1);
        Product updated = productService.findById(productId);
        assertThat(updated.getStock()).isGreaterThanOrEqualTo(0);
    }

    @DisplayName("동시에 같은 쿠폰으로 주문하면, 쿠폰이 정확히 1번만 사용된다.")
    @Test
    void concurrentOrders_couponIsUsedOnlyOnce() throws Exception {
        // arrange
        Product product = productService.create(brandId, "운동화", null, 5000, 100, SellingStatus.SELLING);
        CouponTemplate template = couponService.saveTemplate(
            new CouponTemplate("정액 할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
        UserCoupon userCoupon = couponService.issue(userId, template.getId());
        Long productId = product.getId();
        Long userCouponId = userCoupon.getId();

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(THREAD_COUNT, () -> orderApplicationService.create(userId, productId, 1, userCouponId));

        // assert
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(THREAD_COUNT - 1);
        UserCoupon used = couponService.findUserCouponById(userCouponId);
        assertThat(used.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @DisplayName("결제 성공/실패 콜백이 동시에 호출되면, 정확히 1건만 성공한다.")
    @Test
    void simultaneousConfirmAndFail_exactlyOneSucceeds() throws Exception {
        // arrange
        Product product = productService.create(brandId, "한정판 운동화", null, 10000, 1, SellingStatus.SELLING);
        Long productId = product.getId();
        Order order = orderApplicationService.create(userId, productId, 1, null);
        Long orderId = order.getId();

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(List.of(
            () -> { orderApplicationService.confirmPayment(orderId); return null; },
            () -> { orderApplicationService.failPayment(orderId); return null; }
        ));

        // assert
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);

        Order finalOrder = orderApplicationService.findById(orderId);
        assertThat(finalOrder.getStatus()).isIn(OrderStatus.ORDERED, OrderStatus.PAYMENT_FAILED);
        assertThat(finalOrder.getStatus()).isNotEqualTo(OrderStatus.PAYMENT_PENDING);

        Product finalProduct = productService.findById(productId);
        if (finalOrder.getStatus() == OrderStatus.PAYMENT_FAILED) {
            assertThat(finalProduct.getStock()).isEqualTo(1);
        } else {
            assertThat(finalProduct.getStock()).isEqualTo(0);
        }
    }

    @DisplayName("결제 실패 콜백이 중복 호출되면, 재고와 쿠폰이 정확히 1번만 복원된다.")
    @Test
    void duplicateFailPayment_stockAndCouponRestoredOnlyOnce() throws Exception {
        // arrange
        Product product = productService.create(brandId, "운동화", null, 5000, 1, SellingStatus.SELLING);
        Long productId = product.getId();
        CouponTemplate template = couponService.saveTemplate(
            new CouponTemplate("정액 할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
        UserCoupon userCoupon = couponService.issue(userId, template.getId());
        Long userCouponId = userCoupon.getId();
        Order order = orderApplicationService.create(userId, productId, 1, userCouponId);
        Long orderId = order.getId();

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(List.of(
            () -> { orderApplicationService.failPayment(orderId); return null; },
            () -> { orderApplicationService.failPayment(orderId); return null; }
        ));

        // assert
        assertThat(result.successCount()).isEqualTo(2);

        Product finalProduct = productService.findById(productId);
        assertThat(finalProduct.getStock()).isEqualTo(1);

        UserCoupon finalCoupon = couponService.findUserCouponById(userCouponId);
        assertThat(finalCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    }
}
