package com.growmighty.lectures.firstday.tangledmonolith.settlement.application;

import com.growmighty.lectures.firstday.tangledmonolith.settlement.application.dto.SettleReport;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.support.HeapMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Service;

/**
 * [Step2] 정산 배치 Job 실행기.
 *
 * <p>{@code spring.batch.job.enabled=false} 라 부팅 시 자동 실행되지 않으므로,
 * 이 서비스가 {@link JobOperator} 로 직접 실행한다. 매 호출마다 고유한 JobParameters 를 주어
 * (timestamp) 같은 Job 을 반복 실행할 수 있게 한다.
 * (Batch 6 에서 JobLauncher 는 deprecated → JobOperator 사용)
 *
 * <p>Step1 의 naive 와 동일하게 {@link HeapMonitor} 로 감싸, 배치는 chunk 크기만큼만 메모리를
 * 쓰며 <b>피크 힙이 일정하게 유지</b>되는 걸 대비해서 보여준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final JobOperator jobOperator;
    private final Job settlementJob;

    public SettleReport run() {
        try (HeapMonitor monitor = HeapMonitor.start("batch", 500)) {
            long startedAt = System.currentTimeMillis();

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.warn("[BATCH] 정산 Job 실행");
            JobExecution execution = jobOperator.start(settlementJob, params);

            long read = 0;
            long written = 0;
            for (StepExecution step : execution.getStepExecutions()) {
                read += step.getReadCount();
                written += step.getWriteCount();
            }
            long elapsed = System.currentTimeMillis() - startedAt;

            SettleReport report = new SettleReport(
                    read, written, elapsed, monitor.peakUsedMb(), monitor.maxHeapMb());
            log.warn("[BATCH] 완료. status={}, 리포트={}", execution.getStatus(), report);
            return report;
        } catch (Exception e) {
            throw new IllegalStateException("정산 배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
