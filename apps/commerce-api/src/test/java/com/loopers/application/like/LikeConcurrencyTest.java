package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    private static final int THREAD_COUNT = 5;

    @Autowired
    private LikeApplicationService likeApplicationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

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

    @DisplayName("서로 다른 유저 N명이 동시에 좋아요를 누르면, 모두 성공하고 likeCount == N이다.")
    @Test
    void concurrentLike_allSucceedAndLikeCountIsN() throws Exception {
        // arrange
        Brand brand = brandService.create("Nike", null);
        Product product = productService.create(brand.getId(), "운동화", null, 10000, 100, SellingStatus.SELLING);
        Long productId = product.getId();

        List<Long> userIds = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> {
                UserModel user = userJpaRepository.save(new UserModel(
                    "likeuser" + i, "encoded", "좋아요유저" + i, LocalDate.of(1990, 1, 1), "likeuser" + i + "@test.com"
                ));
                return user.getId();
            })
            .toList();

        // act
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> executor.submit(toCallable(ready, start, () -> likeApplicationService.like(userIds.get(i), productId))))
            .toList();

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = countSuccesses(futures);

        // assert
        assertThat(successCount).isEqualTo(THREAD_COUNT);
        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(THREAD_COUNT);
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
