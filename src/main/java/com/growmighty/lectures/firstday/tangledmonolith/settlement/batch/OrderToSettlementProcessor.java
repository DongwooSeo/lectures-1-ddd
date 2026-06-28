package com.growmighty.lectures.firstday.tangledmonolith.settlement.batch;

import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderStatus;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.domain.Settlement;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * [ItemProcessor] 읽어온 주문 1건을 정산 1건으로 "가공"한다.
 *
 * <p>Chunk 지향 처리의 가운데 단계. Reader 가 준 {@link Order} 를 받아
 * {@link Settlement} 로 변환한다. 1원 무결성 계산은 {@link Settlement#of} 에 그대로 위임한다.
 *
 * <p>변환할 수 없는 주문(결제 안 됨/결제 id 없음)은 {@code null} 을 반환한다.
 * Spring Batch 는 Processor 가 {@code null} 을 반환하면 그 아이템을 <b>필터링</b>(Writer 로 안 넘김)한다.
 */
@Component
public class OrderToSettlementProcessor implements ItemProcessor<Order, Settlement> {

    /** 플랫폼 수수료율 3% */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.03");

    @Override
    public Settlement process(Order order) {
        if (order.getStatus() != OrderStatus.PAID || order.getPaymentId() == null) {
            return null; // 정산 대상 아님 → 필터링
        }
        BigDecimal amount = order.getTotalAmount().getValue();
        return Settlement.of(order.getId(), order.getPaymentId(), amount, FEE_RATE);
    }
}
