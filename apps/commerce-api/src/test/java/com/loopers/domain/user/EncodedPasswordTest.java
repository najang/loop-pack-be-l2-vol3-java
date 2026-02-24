package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncodedPasswordTest {

    @DisplayName("EncodedPassword 생성 시,")
    @Nested
    class Create {

        @DisplayName("유효한 인코딩된 비밀번호가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsEncodedPassword_whenValueIsValid() {
            // arrange & act
            EncodedPassword password = new EncodedPassword("encodedPassword123");

            // assert
            assertThat(password.getValue()).isEqualTo("encodedPassword123");
        }

        @DisplayName("null이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new EncodedPassword(null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("빈 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new EncodedPassword(""))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("공백 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsWhitespace() {
            // arrange & act & assert
            assertThatThrownBy(() -> new EncodedPassword("   "))
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
            EncodedPassword pw1 = new EncodedPassword("encoded123");
            EncodedPassword pw2 = new EncodedPassword("encoded123");

            // act & assert
            assertThat(pw1).isEqualTo(pw2);
        }
    }
}
