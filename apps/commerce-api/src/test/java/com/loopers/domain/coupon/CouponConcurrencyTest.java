package com.loopers.domain.coupon;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
class CouponConcurrencyTest {

    private static final int THREAD_COUNT = 5;
    private static final Long USER_ID = 1L;

    @Autowired
    private CouponService couponService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동시에 같은 쿠폰을 중복 발급하면, 정확히 1건만 성공한다.")
    @Test
    void concurrentIssue_onlyOneSucceeds() throws Exception {
        // arrange
        CouponTemplate template = couponService.saveTemplate(
            new CouponTemplate("동시성 테스트 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
        Long templateId = template.getId();

        // act
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> executor.submit(toCallable(ready, start, () -> couponService.issue(USER_ID, templateId))))
            .toList();

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = futures.stream()
            .mapToLong(f -> {
                try {
                    return f.get() ? 1L : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            })
            .sum();

        // assert
        assertThat(successCount).isEqualTo(1);
        List<UserCoupon> issued = couponService.findByUserId(USER_ID);
        assertThat(issued).hasSize(1);
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
}
