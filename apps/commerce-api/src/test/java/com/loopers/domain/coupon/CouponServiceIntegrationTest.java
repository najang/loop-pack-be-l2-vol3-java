package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CouponServiceIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private CouponService couponService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private CouponTemplate saveActiveTemplate() {
        return couponService.saveTemplate(
            new CouponTemplate("할인 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
    }

    @DisplayName("쿠폰 템플릿 저장 시,")
    @Nested
    class SaveTemplate {

        @DisplayName("정상 저장되면, ID가 발급된다.")
        @Test
        void savesTemplate_withId() {
            // arrange
            CouponTemplate template = new CouponTemplate("할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7));

            // act
            CouponTemplate saved = couponService.saveTemplate(template);

            // assert
            assertThat(saved.getId()).isNotNull();
        }
    }

    @DisplayName("쿠폰 템플릿 목록 조회 시,")
    @Nested
    class FindAllTemplates {

        @DisplayName("저장된 템플릿 목록이 페이징으로 반환된다.")
        @Test
        void returnsTemplates_withPaging() {
            // arrange
            couponService.saveTemplate(new CouponTemplate("A", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)));
            couponService.saveTemplate(new CouponTemplate("B", CouponType.RATE, 10, null, ZonedDateTime.now().plusDays(7)));

            // act
            Page<CouponTemplate> result = couponService.findAllTemplates(PageRequest.of(0, 20));

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @DisplayName("soft delete된 템플릿은 목록에서 제외된다.")
        @Test
        void excludesDeletedTemplates() {
            // arrange
            CouponTemplate template = saveActiveTemplate();
            template.delete();
            couponService.saveTemplate(template);

            // act
            Page<CouponTemplate> result = couponService.findAllTemplates(PageRequest.of(0, 20));

            // assert
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @DisplayName("쿠폰 발급 시,")
    @Nested
    class Issue {

        @DisplayName("활성 템플릿이면, UserCoupon이 AVAILABLE 상태로 발급된다.")
        @Test
        void issuesCoupon_withAvailableStatus() {
            // arrange
            CouponTemplate template = saveActiveTemplate();

            // act
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());

            // assert
            assertAll(
                () -> assertThat(userCoupon.getId()).isNotNull(),
                () -> assertThat(userCoupon.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(userCoupon.getCouponTemplateId()).isEqualTo(template.getId()),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE)
            );
        }

        @DisplayName("비활성 템플릿이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTemplateIsInactive() {
            // arrange
            CouponTemplate template = saveActiveTemplate();
            template.deactivate();
            couponService.saveTemplate(template);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> couponService.issue(USER_ID, template.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 템플릿이면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenTemplateNotFound() {
            // act
            CoreException ex = assertThrows(CoreException.class, () -> couponService.issue(USER_ID, 99999L));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("만료된 템플릿이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTemplateIsExpired() {
            // arrange
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("만료 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().minusDays(1))
            );

            // act
            CoreException ex = assertThrows(CoreException.class, () -> couponService.issue(USER_ID, template.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("사용자 쿠폰 목록 조회 시,")
    @Nested
    class FindByUserId {

        @DisplayName("해당 userId의 쿠폰만 반환된다.")
        @Test
        void returnsOnlyUserCoupons() {
            // arrange
            CouponTemplate template = saveActiveTemplate();
            couponService.issue(USER_ID, template.getId());
            couponService.issue(USER_ID, template.getId());
            couponService.issue(OTHER_USER_ID, template.getId());

            // act
            List<UserCoupon> result = couponService.findByUserId(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(uc -> uc.getUserId().equals(USER_ID));
        }
    }

    @DisplayName("쿠폰 검증 및 사용 처리 시,")
    @Nested
    class ValidateAndUse {

        @DisplayName("유효한 쿠폰이면, 할인액을 반환하고 상태가 USED가 된다.")
        @Test
        void returnsDiscountAndMarksUsed_whenValid() {
            // arrange
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("정액 할인", CouponType.FIXED, 3000, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());

            // act
            int discount = couponService.validateAndUse(USER_ID, userCoupon.getId(), 20000);

            // assert
            assertThat(discount).isEqualTo(3000);

            UserCoupon used = couponService.findUserCouponById(userCoupon.getId());
            assertThat(used.getStatus()).isEqualTo(UserCouponStatus.USED);
        }

        @DisplayName("타 유저의 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponBelongsToOtherUser() {
            // arrange
            CouponTemplate template = saveActiveTemplate();
            UserCoupon userCoupon = couponService.issue(OTHER_USER_ID, template.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> couponService.validateAndUse(USER_ID, userCoupon.getId(), 10000));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("최소 주문 금액 미충족이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenMinOrderAmountNotMet() {
            // arrange
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("조건 할인", CouponType.FIXED, 1000, 50000, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> couponService.validateAndUse(USER_ID, userCoupon.getId(), 10000));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
