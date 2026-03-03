package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @DisplayName("Email 생성 시,")
    @Nested
    class Create {

        @DisplayName("유효한 이메일이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsEmail_whenValueIsValid() {
            // arrange & act
            Email email = new Email("test@example.com");

            // assert
            assertThat(email.getValue()).isEqualTo("test@example.com");
        }

        @DisplayName("null이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("빈 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("@가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAtSignIsMissing() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Email("invalid-email"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST))
                .hasMessageContaining("이메일");
        }

        @DisplayName("도메인 확장자가 1자리면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTldIsTooShort() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Email("test@example.c"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("공백이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenContainsWhitespace() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Email("test @example.com"))
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
            Email email1 = new Email("test@example.com");
            Email email2 = new Email("test@example.com");

            // act & assert
            assertThat(email1).isEqualTo(email2);
        }
    }
}
