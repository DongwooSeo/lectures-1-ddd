package com.growmighty.lectures.firstday.tangledmonolith.settlement.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderRepository;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.application.NaiveSettlementService;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.application.dto.SettleReport;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.domain.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 정산 실습용 트리거 엔드포인트. (Step1 - "배치가 필요한 이유")
 *
 * <ul>
 *   <li>POST /settlements/naive          : [데모1] findAll 전량 적재 → 대용량에서 즉시 OOM</li>
 *   <li>POST /settlements/naive?limit=N  : [데모2] N건까지 메모리에 쌓으며 정산 → 메모리/시간 증가 추세 관찰</li>
 *   <li>GET  /settlements/status         : 주문/정산 건수 + 현재 힙 상태 확인</li>
 *   <li>DELETE /settlements              : 정산 결과 비우기 (데모 반복용)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
public class SettlementController {

    private static final long MB = 1024 * 1024;

    private final NaiveSettlementService naiveSettlementService;
    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;

    /**
     * [데모1/데모2] limit 이 없으면 findAll 전량 적재(OOM 데모),
     * limit 이 있으면 그 수만큼만 메모리에 쌓으며 정산(추세 관찰).
     */
    @PostMapping("/naive")
    public ApiResponse<SettleReport> settleNaive(@RequestParam(required = false) Integer limit) {
        SettleReport report = (limit == null)
                ? naiveSettlementService.settleAll()
                : naiveSettlementService.settleUpTo(limit);
        return ApiResponse.ok(report);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / MB;
        long maxMb = rt.maxMemory() / MB;
        return ApiResponse.ok(Map.of(
                "orderCount", orderRepository.count(),
                "settlementCount", settlementRepository.count(),
                "heapUsedMb", usedMb,
                "heapMaxMb", maxMb));
    }

    @DeleteMapping
    public ApiResponse<Void> clear() {
        settlementRepository.deleteAll();
        return ApiResponse.ok();
    }
}
