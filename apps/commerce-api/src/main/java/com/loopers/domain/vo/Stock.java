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
public class Stock {

    @Column(name = "stock", nullable = false)
    private int value;

    public Stock(int value) {
        validate(value);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Stock deduct(int amount) {
        if (this.value < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new Stock(this.value - amount);
    }

    public Stock restore(int amount) {
        return new Stock(this.value + amount);
    }

    private void validate(int value) {
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
    }
}
