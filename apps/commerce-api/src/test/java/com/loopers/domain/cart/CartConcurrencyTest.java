package com.loopers.domain.cart;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CartConcurrencyTest {

    private static final int THREAD_COUNT = 2;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 같은 상품에 동시에 addQuantity(1)을 2번 요청하면, 1건만 성공하고 최종 수량은 유실되지 않는다.")
    @Test
    void concurrentAddQuantity_onlyOneSucceedsAndQuantityIsCorrect() throws Exception {
        // arrange
        Brand brand = brandService.create("Nike", null);
        Product product = productService.create(brand.getId(), "운동화", null, 10000, 100, SellingStatus.SELLING);
        Long productId = product.getId();

        UserModel user = userJpaRepository.save(new UserModel(
            "cartuser", "encoded", "장바구니유저", LocalDate.of(1990, 1, 1), "cartuser@test.com"
        ));
        Long userId = user.getId();

        // 초기 장바구니 생성 (수량 1)
        cartService.add(userId, productId, 1);

        // act
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = List.of(
            executor.submit(toCallable(ready, start, () -> cartService.add(userId, productId, 1))),
            executor.submit(toCallable(ready, start, () -> cartService.add(userId, productId, 1)))
        );

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = countSuccesses(futures);

        // assert — 1건 성공, 1건 실패, 최종 수량 == 2 (1 + 1)
        assertThat(successCount).isEqualTo(1);
        Cart cart = cartRepository.findByUserIdAndProductId(userId, productId).orElseThrow();
        assertThat(cart.getQuantity()).isEqualTo(2);
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
