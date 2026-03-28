package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.event.UserActionEvent;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
@SpringBootTest
class CouponFacadeEventIntegrationTest {

    @Autowired
    private CouponFacade couponFacade;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ApplicationEvents applicationEvents;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 발급 시, COUPON_ISSUED UserActionEvent가 발행된다.")
    @Test
    void publishesCouponIssuedEvent_whenCouponIsIssued() {
        // arrange
        CouponTemplate template = couponTemplateJpaRepository.save(
            new CouponTemplate("테스트 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );

        // act
        UserCouponInfo info = couponFacade.issue(1L, template.getId());

        // assert
        long count = applicationEvents.stream(UserActionEvent.class)
            .filter(e -> e.eventType() == UserActionEvent.EventType.COUPON_ISSUED
                && e.userId().equals(1L)
                && e.targetId().equals(info.id()))
            .count();
        assertThat(count).isEqualTo(1);
    }
}
