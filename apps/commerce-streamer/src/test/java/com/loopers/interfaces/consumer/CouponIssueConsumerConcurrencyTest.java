package com.loopers.interfaces.consumer;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.infrastructure.coupon.CouponIssueResultStreamerJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponStreamerJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "spring.kafka.listener.auto-startup=false"
})
class CouponIssueConsumerConcurrencyTest {

    private static final int MAX_QUANTITY = 100;
    private static final int TOTAL_REQUESTS = 200;

    @Autowired
    private CouponIssueConsumer couponIssueConsumer;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private CouponIssueResultStreamerJpaRepository couponIssueResultRepository;

    @Autowired
    private UserCouponStreamerJpaRepository userCouponRepository;

    private Long couponTemplateId;
    private List<String> requestIds;

    @BeforeEach
    void setUp() {
        CouponTemplate template = couponTemplateJpaRepository.save(new CouponTemplate("선착순쿠폰", MAX_QUANTITY));
        couponTemplateId = template.getId();

        requestIds = new ArrayList<>();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            String requestId = UUID.randomUUID().toString();
            requestIds.add(requestId);
            couponIssueResultRepository.save(new CouponIssueResult(requestId, (long) (i + 1), couponTemplateId));
        }
    }

    @AfterEach
    void tearDown() {
        userCouponRepository.deleteAll();
        couponIssueResultRepository.deleteAll();
        couponTemplateJpaRepository.deleteAll();
    }

    @DisplayName("선착순 쿠폰 200명 동시 요청 시, 정확히 100명만 SUCCESS가 된다.")
    @Test
    void concurrentCouponIssue_exactlyMaxQuantitySucceed() throws InterruptedException {
        int threadCount = TOTAL_REQUESTS;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    String requestId = requestIds.get(idx);
                    CouponIssueConsumer.CouponIssueMessage message = new CouponIssueConsumer.CouponIssueMessage(
                        requestId, (long) (idx + 1), couponTemplateId, MAX_QUANTITY
                    );
                    ConsumerRecord<String, CouponIssueConsumer.CouponIssueMessage> record =
                        new ConsumerRecord<>("coupon-issue-requests", 0, idx, String.valueOf(couponTemplateId), message);
                    Acknowledgment ack = mock(Acknowledgment.class);

                    couponIssueConsumer.handleCouponIssueEvents(List.of(record), ack);
                } catch (Exception e) {
                    // 개별 실패는 무시
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 검증
        List<CouponIssueResult> results = couponIssueResultRepository.findAll();
        long successCount = results.stream().filter(r -> r.getStatus() == CouponIssueStatus.SUCCESS).count();
        long nonSuccessCount = results.stream().filter(r -> r.getStatus() != CouponIssueStatus.SUCCESS && r.getStatus() != CouponIssueStatus.PENDING).count();
        int issuedCount = couponTemplateJpaRepository.findIssuedCountById(couponTemplateId);

        assertThat(successCount).isEqualTo(MAX_QUANTITY);
        assertThat(successCount + nonSuccessCount).isEqualTo(TOTAL_REQUESTS);
        assertThat(issuedCount).isEqualTo(MAX_QUANTITY);
    }
}
