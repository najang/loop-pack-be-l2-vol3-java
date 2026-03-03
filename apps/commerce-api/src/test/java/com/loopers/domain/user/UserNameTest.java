package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserNameTest {

    @DisplayName("UserName 생성 시,")
    @Nested
    class Create {

        @DisplayName("유효한 이름이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsUserName_whenValueIsValid() {
            // arrange & act
            UserName name = new UserName("홍길동");

            // assert
            assertThat(name.getValue()).isEqualTo("홍길동");
        }

        @DisplayName("null이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserName(null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("빈 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new UserName(""))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("이름 마스킹 시,")
    @Nested
    class Masked {

        @DisplayName("이름이 2글자 이상이면, 마지막 글자를 *로 마스킹한다.")
        @Test
        void masksLastCharacter_whenNameHasMultipleCharacters() {
            // arrange
            UserName name = new UserName("홍길동");

            // act
            String masked = name.masked();

            // assert
            assertThat(masked).isEqualTo("홍길*");
        }

        @DisplayName("이름이 1글자면, 전체를 *로 마스킹한다.")
        @Test
        void masksEntireName_whenNameHasOneCharacter() {
            // arrange
            UserName name = new UserName("김");

            // act
            String masked = name.masked();

            // assert
            assertThat(masked).isEqualTo("*");
        }

        @DisplayName("이름이 2글자면, 마지막 글자를 *로 마스킹한다.")
        @Test
        void masksLastCharacter_whenNameHasTwoCharacters() {
            // arrange
            UserName name = new UserName("김철");

            // act
            String masked = name.masked();

            // assert
            assertThat(masked).isEqualTo("김*");
        }
    }

    @DisplayName("동등성 비교 시,")
    @Nested
    class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        void isEqual_whenValueIsSame() {
            // arrange
            UserName name1 = new UserName("홍길동");
            UserName name2 = new UserName("홍길동");

            // act & assert
            assertThat(name1).isEqualTo(name2);
        }
    }
}
