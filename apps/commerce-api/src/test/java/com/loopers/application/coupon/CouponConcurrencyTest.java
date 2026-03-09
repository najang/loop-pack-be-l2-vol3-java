package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.ConcurrencyTestHelper;
import com.loopers.utils.ConcurrencyTestHelper.ConcurrencyResult;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    private static final int THREAD_COUNT = 100;

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

    @DisplayName("동일 유저가 동일 쿠폰에 동시 100개 요청을 보내면, 1건만 성공하고 99건은 실패한다.")
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
        ConcurrencyResult result = ConcurrencyTestHelper.run(THREAD_COUNT, () -> couponService.issue(userId, couponTemplateId));

        // assert — 1건 성공, 99건 실패, DB에 1건만 발급
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(THREAD_COUNT - 1);
        assertThat(couponService.findByUserId(userId)).hasSize(1);
    }
}
