package com.growmighty.lectures.firstday.tangledmonolith.settlement.application.dto;

/**
 * 정산 작업 결과 리포트 - "얼마나 처리했고, 얼마나 걸렸고, 메모리는 얼마나 썼나".
 * 배치 필요성("느림 + 메모리")을 수치로 보여주기 위한 DTO.
 */
public record SettleReport(
        long readCount,     // 메모리로 읽어들인 주문 수
        long settledCount,  // 실제 정산 생성 건수
        long elapsedMs,     // 소요 시간
        long peakHeapMb,    // 작업 중 피크 힙 사용량
        long maxHeapMb      // -Xmx 상한
) {
}
