package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserModelTest {

    private static final String LOGIN_ID = "testuser1";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String NAME = "홍길동";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 15);
    private static final String EMAIL = "test@example.com";

    @DisplayName("유저 모델을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsUserModel_whenAllFieldsAreProvided() {
            // arrange & act
            UserModel userModel = new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL);

            // assert
            assertAll(
                () -> assertThat(userModel.getLoginId()).isEqualTo(LOGIN_ID),
                () -> assertThat(userModel.getPassword()).isEqualTo(ENCODED_PASSWORD),
                () -> assertThat(userModel.getName()).isEqualTo(NAME),
                () -> assertThat(userModel.getBirthDate()).isEqualTo(BIRTH_DATE),
                () -> assertThat(userModel.getEmail()).isEqualTo(EMAIL)
            );
        }

        @DisplayName("로그인 ID가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenLoginIdIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel("", ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel("test@user", ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST))
                .hasMessageContaining("영문과 숫자만");
        }

        @DisplayName("비밀번호가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenPasswordIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel(LOGIN_ID, "", NAME, BIRTH_DATE, EMAIL))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel(LOGIN_ID, ENCODED_PASSWORD, "", BIRTH_DATE, EMAIL))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("생년월일이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBirthDateIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, null, EMAIL))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("이메일이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenEmailIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, ""))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("이메일 형식이 잘못되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenEmailFormatIsInvalid() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, "invalid-email"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST))
                .hasMessageContaining("이메일");
        }
    }

    @DisplayName("이름 마스킹 시,")
    @Nested
    class MaskName {

        @DisplayName("UserName의 masked()에 위임하여 마스킹된 이름을 반환한다.")
        @Test
        void returnsMaskedName_byDelegatingToUserName() {
            // arrange
            UserModel userModel = new UserModel(LOGIN_ID, ENCODED_PASSWORD, "홍길동", BIRTH_DATE, EMAIL);

            // act
            String maskedName = userModel.getMaskedName();

            // assert
            assertThat(maskedName).isEqualTo("홍길*");
        }
    }

    @DisplayName("포인트 충전 시,")
    @Nested
    class ChargePoints {

        @DisplayName("양수 금액을 충전하면, 잔액이 증가한다.")
        @Test
        void increasesBalance_whenAmountIsPositive() {
            // arrange
            UserModel user = new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL);

            // act
            user.chargePoints(5000);

            // assert
            assertThat(user.getPointBalance()).isEqualTo(5000);
        }

        @DisplayName("0 이하 금액을 충전하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAmountIsNotPositive() {
            // arrange
            UserModel user = new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL);

            // act & assert
            assertThatThrownBy(() -> user.chargePoints(0))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("포인트 차감 시,")
    @Nested
    class DeductPoints {

        @DisplayName("잔액이 충분하면, 차감된다.")
        @Test
        void deductsBalance_whenSufficientBalance() {
            // arrange
            UserModel user = new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL);
            user.chargePoints(10000);

            // act
            user.deductPoints(3000);

            // assert
            assertThat(user.getPointBalance()).isEqualTo(7000);
        }

        @DisplayName("잔액이 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenInsufficientBalance() {
            // arrange
            UserModel user = new UserModel(LOGIN_ID, ENCODED_PASSWORD, NAME, BIRTH_DATE, EMAIL);

            // act & assert
            assertThatThrownBy(() -> user.deductPoints(1000))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("비밀번호 변경 시,")
    @Nested
    class ChangePassword {
        @DisplayName("새로운 비밀번호가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNewPasswordIsBlank() {
            // arrange
            UserModel userModel = new UserModel(LOGIN_ID, "oldEncoded", NAME, BIRTH_DATE, EMAIL);

            // act & assert
            assertThatThrownBy(() -> userModel.changePassword(""))
                    .isInstanceOf(CoreException.class)
                    .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("새로운 암호화된 비밀번호로 변경된다.")
        @Test
        void changesPassword_whenNewEncodedPasswordIsProvided() {
            // arrange
            UserModel userModel = new UserModel(LOGIN_ID, "oldEncoded", NAME, BIRTH_DATE, EMAIL);
            String newEncodedPassword = "newEncodedPassword";

            // act
            userModel.changePassword(newEncodedPassword);

            // assert
            assertThat(userModel.getPassword()).isEqualTo(newEncodedPassword);
        }
    }
}
