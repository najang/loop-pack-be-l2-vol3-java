package com.loopers.domain.order;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    private static final int THREAD_COUNT = 2;

    private Long brandId;
    private Long userId;

    @Autowired
    private OrderService orderService;

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
        long successCount = runConcurrently(() -> orderService.create(userId, productId, 1, null));

        // assert
        assertThat(successCount).isEqualTo(1);
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
        long successCount = runConcurrently(() -> orderService.create(userId, productId, 1, userCouponId));

        // assert
        assertThat(successCount).isEqualTo(1);
        UserCoupon used = couponService.findUserCouponById(userCouponId);
        assertThat(used.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @DisplayName("포인트 잔액이 부족한 상태에서 서로 다른 상품을 동시에 주문하면, 정확히 1건만 성공한다.")
    @Test
    void concurrentOrders_onlyOneSucceedsWhenPointBalanceIsInsufficient() throws Exception {
        // arrange — 잔액 10000, 각 주문 8000 → 동시 차감 시 1건만 성공해야 함
        userService.chargePoints(userId, 10000);
        Product productA = productService.create(brandId, "운동화 A", null, 8000, 100, SellingStatus.SELLING);
        Product productB = productService.create(brandId, "운동화 B", null, 8000, 100, SellingStatus.SELLING);
        Long productAId = productA.getId();
        Long productBId = productB.getId();

        // act — 서로 다른 상품이므로 상품 락으로 직렬화되지 않아 포인트 낙관적 락이 동작함
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = List.of(
            executor.submit(toCallable(ready, start, () -> orderService.create(userId, productAId, 1, null))),
            executor.submit(toCallable(ready, start, () -> orderService.create(userId, productBId, 1, null)))
        );

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = countSuccesses(futures);

        // assert
        assertThat(successCount).isEqualTo(1);
        UserModel user = userJpaRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(0);
    }

    private long runConcurrently(Callable<Object> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> executor.submit(toCallable(ready, start, task)))
            .toList();

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        return countSuccesses(futures);
    }

    private Callable<Boolean> toCallable(CountDownLatch ready, CountDownLatch start, Callable<Object> task) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                task.call();
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private long countSuccesses(List<Future<Boolean>> futures) {
        return futures.stream()
            .mapToLong(f -> {
                try {
                    return f.get() ? 1L : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            })
            .sum();
    }
}
