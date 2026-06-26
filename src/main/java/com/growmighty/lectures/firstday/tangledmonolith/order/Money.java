package com.growmighty.lectures.firstday.tangledmonolith.order;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private BigDecimal value;

    private Money(BigDecimal value) {
        this.value = value;
    }

    public static Money from(BigDecimal value) {
        Objects.requireNonNull(value, "금액은 null일 수 없습니다.");

        if (value.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다. 입력값: " + value);

        return new Money(value);
    }
}
