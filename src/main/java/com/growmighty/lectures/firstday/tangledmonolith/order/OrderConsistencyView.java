package com.growmighty.lectures.firstday.tangledmonolith.order;

import java.math.BigDecimal;

// 주문에 저장된 총액(storedTotal)이랑 항목들로 다시 더한 총액(recalculatedTotal)을 비교해보는 용도.
// 항목을 Order 안 거치고 직접 건드리면 둘이 어긋나서 consistent 가 false 로 나온다.
public record OrderConsistencyView(
        Long orderId,
        BigDecimal storedTotal,
        BigDecimal recalculatedTotal,
        boolean consistent
) {
}
