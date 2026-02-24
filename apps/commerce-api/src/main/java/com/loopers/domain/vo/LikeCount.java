package com.loopers.domain.vo;

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
public class LikeCount {

    @Column(name = "like_count", nullable = false)
    private int value;

    public LikeCount(int value) {
        validate(value);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public LikeCount increase() {
        return new LikeCount(this.value + 1);
    }

    public LikeCount decrease() {
        if (this.value <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 미만이 될 수 없습니다.");
        }
        return new LikeCount(this.value - 1);
    }

    private void validate(int value) {
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.");
        }
    }
}
