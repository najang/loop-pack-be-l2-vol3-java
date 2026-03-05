package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserName {

    @Column(name = "name", nullable = false)
    private String value;

    public UserName(String value) {
        validate(value);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String masked() {
        if (value.length() == 1) {
            return "*";
        }
        return value.substring(0, value.length() - 1) + "*";
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
    }
}
