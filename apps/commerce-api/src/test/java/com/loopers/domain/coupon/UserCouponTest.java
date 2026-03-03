package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserCouponTest {

    private static final Long USER_ID = 1L;
    private static final Long TEMPLATE_ID = 10L;

    private CouponTemplate activeTemplate(int minOrderAmount) {
        return new CouponTemplate("할인", CouponType.FIXED, 1000, minOrderAmount, ZonedDateTime.now().plusDays(7));
    }

    private CouponTemplate activeTemplate() {
        return activeTemplate(0);
    }

    @DisplayName("UserCoupon을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("정상 입력이면, status=AVAILABLE, usedAt=null로 생성된다.")
        @Test
        void createsUserCoupon_withAvailableStatus() {
            // act
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);

            // assert
            assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(userCoupon.getCouponTemplateId()).isEqualTo(TEMPLATE_ID),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE),
                () -> assertThat(userCoupon.getUsedAt()).isNull()
            );
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new UserCoupon(null, TEMPLATE_ID))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("couponTemplateId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponTemplateIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new UserCoupon(USER_ID, null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("use()를 호출할 때,")
    @Nested
    class Use {

        @DisplayName("AVAILABLE 상태이면, status=USED, usedAt이 설정된다.")
        @Test
        void setsUsedStatus_whenAvailable() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);

            // act
            userCoupon.use();

            // assert
            assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED),
                () -> assertThat(userCoupon.getUsedAt()).isNotNull()
            );
        }

        @DisplayName("이미 USED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAlreadyUsed() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            userCoupon.use();

            // act & assert
            assertThatThrownBy(userCoupon::use)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("canUse()를 검증할 때,")
    @Nested
    class CanUse {

        @DisplayName("AVAILABLE 상태이고 minOrderAmount 조건을 충족하면, 예외가 발생하지 않는다.")
        @Test
        void doesNotThrow_whenAvailableAndMinAmountMet() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            CouponTemplate template = activeTemplate(10000);

            // act & assert (no exception)
            userCoupon.canUse(10000, template);
        }

        @DisplayName("USED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsUsed() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            userCoupon.use();
            CouponTemplate template = activeTemplate();

            // act & assert
            assertThatThrownBy(() -> userCoupon.canUse(50000, template))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("EXPIRED 상태이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsExpired() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            userCoupon.expire();
            CouponTemplate template = activeTemplate();

            // act & assert
            assertThatThrownBy(() -> userCoupon.canUse(50000, template))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("minOrderAmount 조건을 충족하지 못하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenMinOrderAmountNotMet() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            CouponTemplate template = activeTemplate(20000);

            // act & assert
            assertThatThrownBy(() -> userCoupon.canUse(10000, template))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("minOrderAmount가 null이면, 조건 없이 사용 가능하다.")
        @Test
        void doesNotThrow_whenMinOrderAmountIsNull() {
            // arrange
            UserCoupon userCoupon = new UserCoupon(USER_ID, TEMPLATE_ID);
            CouponTemplate template = new CouponTemplate("할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7));

            // act & assert (no exception)
            userCoupon.canUse(1000, template);
        }
    }
}
