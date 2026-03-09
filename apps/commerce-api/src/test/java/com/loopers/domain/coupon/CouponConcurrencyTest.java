package com.loopers.domain.coupon;

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
import java.util.List;

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

    @DisplayName("동시에 같은 쿠폰을 중복 발급하면, 정확히 1건만 성공한다.")
    @Test
    void concurrentIssue_onlyOneSucceeds() throws Exception {
        // arrange
        UserModel user = userJpaRepository.save(new UserModel(
            "couponuser", "encoded", "쿠폰유저", LocalDate.of(1990, 1, 1), "couponuser@test.com"
        ));
        Long userId = user.getId();

        CouponTemplate template = couponService.saveTemplate(
            new CouponTemplate("동시성 테스트 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
        Long templateId = template.getId();

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(THREAD_COUNT, () -> couponService.issue(userId, templateId));

        // assert
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(THREAD_COUNT - 1);
        List<UserCoupon> issued = couponService.findByUserId(userId);
        assertThat(issued).hasSize(1);
    }
}
