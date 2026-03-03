package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginIdTest {

    @DisplayName("LoginId 생성 시,")
    @Nested
    class Create {

        @DisplayName("유효한 영문+숫자 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsLoginId_whenValueIsValid() {
            // arrange & act
            LoginId loginId = new LoginId("testuser1");

            // assert
            assertThat(loginId.getValue()).isEqualTo("testuser1");
        }

        @DisplayName("null이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new LoginId(null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("빈 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new LoginId(""))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueContainsSpecialCharacters() {
            // arrange & act & assert
            assertThatThrownBy(() -> new LoginId("test@user"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST))
                .hasMessageContaining("영문과 숫자만");
        }

        @DisplayName("한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueContainsKorean() {
            // arrange & act & assert
            assertThatThrownBy(() -> new LoginId("테스트user"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("동등성 비교 시,")
    @Nested
    class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        void isEqual_whenValueIsSame() {
            // arrange
            LoginId loginId1 = new LoginId("testuser1");
            LoginId loginId2 = new LoginId("testuser1");

            // act & assert
            assertThat(loginId1).isEqualTo(loginId2);
        }

        @DisplayName("다른 값이면 동등하지 않다.")
        @Test
        void isNotEqual_whenValueIsDifferent() {
            // arrange
            LoginId loginId1 = new LoginId("testuser1");
            LoginId loginId2 = new LoginId("testuser2");

            // act & assert
            assertThat(loginId1).isNotEqualTo(loginId2);
        }
    }
}
