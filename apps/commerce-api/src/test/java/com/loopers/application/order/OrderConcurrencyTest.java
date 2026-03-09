package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
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
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

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
    private UserService userService;

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
        userService.chargePoints(userId, 10000);
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
        userService.chargePoints(userId, 100000);
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

    @DisplayName("포인트 잔액이 부족한 상태에서 서로 다른 상품을 동시에 주문하면, 정확히 1건만 성공한다.")
    @Test
    void concurrentOrders_onlyOneSucceedsWhenPointBalanceIsInsufficient() throws Exception {
        // arrange — 잔액 10000, 각 주문 8000 → 동시 차감 시 1건만 성공해야 함
        userService.chargePoints(userId, 10000);
        List<Long> productIds = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> productService.create(brandId, "운동화 " + i, null, 8000, 100, SellingStatus.SELLING).getId())
            .toList();

        // act — 서로 다른 상품이므로 상품 락으로 직렬화되지 않아 포인트 낙관적 락이 동작함
        List<Callable<Object>> tasks = IntStream.range(0, THREAD_COUNT)
            .<Callable<Object>>mapToObj(i -> () -> orderApplicationService.create(userId, productIds.get(i), 1, null))
            .toList();
        ConcurrencyResult result = ConcurrencyTestHelper.run(tasks);

        // assert
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(THREAD_COUNT - 1);
        UserModel user = userJpaRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(0);
    }
}
