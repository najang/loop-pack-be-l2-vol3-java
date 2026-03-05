package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    private static final int THREAD_COUNT = 2;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 동일 쿠폰에 동시 2개 요청을 보내면, 1건만 성공하고 1건은 실패한다.")
    @Test
    void concurrentIssue_onlyOneSucceeds() throws Exception {
        // arrange
        UserModel user = userJpaRepository.save(new UserModel(
            "couponuser", "encoded", "쿠폰유저", LocalDate.of(1990, 1, 1), "couponuser@test.com"
        ));
        Long userId = user.getId();

        CouponTemplate template = couponService.saveTemplate(new CouponTemplate(
            "테스트쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)
        ));
        Long couponTemplateId = template.getId();

        // act
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Future<Boolean>> futures = List.of(
            executor.submit(toCallable(ready, start, () -> couponService.issue(userId, couponTemplateId))),
            executor.submit(toCallable(ready, start, () -> couponService.issue(userId, couponTemplateId)))
        );

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = countSuccesses(futures);

        // assert — 1건 성공, 1건 실패, DB에 1건만 발급
        assertThat(successCount).isEqualTo(1);
        assertThat(couponService.findByUserId(userId)).hasSize(1);
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
