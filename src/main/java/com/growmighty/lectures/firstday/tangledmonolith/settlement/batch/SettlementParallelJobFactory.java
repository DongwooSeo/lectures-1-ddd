package com.growmighty.lectures.firstday.tangledmonolith.settlement.batch;

import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.settlement.domain.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [Step4] 멀티스레드 Step / 파티셔닝 Job 을 <b>런타임 스레드 수·gridSize 로</b> 조립하는 팩토리.
 *
 * <p>왜 빈으로 고정하지 않고 매번 만드나? 라이브 세션에서 "스레드를 4 → 8 → 16 으로 올려 가며"
 * 처리량/장애를 관찰하려면, 병렬도를 요청마다 바꿔 끼울 수 있어야 하기 때문이다.
 * 빌더로 Step/Job 을 즉석에서 만들고 {@code JobOperator.start(job, params)} 로 실행한다.
 */
@Component
@RequiredArgsConstructor
public class SettlementParallelJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderToSettlementProcessor settlementProcessor;
    private final JpaItemWriter<Settlement> settlementWriter;
    private final UnsafeSharedOrderReader unsafeSharedOrderReader;
    /**
     * Step2 의 표준 페이징 Reader(@StepScope). Batch 6 라 read() 가 내부 Lock 으로 thread-safe → 안전 데모용.
     * 필드명을 빈 이름과 똑같이 둬서(다른 JpaPagingItemReader 빈과의 모호성을) 이름으로 해소한다.
     */
    private final JpaPagingItemReader<Order> settlementOrderReader;
    private final Step settlementWorkerStep;
    private final JdbcTemplate jdbcTemplate;

    @Value("${settlement.batch.chunk-size:1000}")
    private int chunkSize;

    /**
     * [Step4-1 · 4-2] 함정 그 자체 — 전통적 Multi-threaded Step.
     *
     * <p>Chunk 들을 {@code threads} 개 스레드가 <b>각자 통째로</b> 처리한다(read+process+write).
     * 그래서 여러 스레드가 {@link UnsafeSharedOrderReader} <b>하나를 동시에</b> 호출한다 — 이게 함정이다.
     * <ul>
     *   <li><b>[4-1]</b> 스레드 수 &gt; HikariCP {@code maximumPoolSize} 면 커넥션 고갈 → 타임아웃.</li>
     *   <li><b>[4-2]</b> 풀을 키워 에러를 없애도, 공유 Reader 경쟁으로 정산 건수가 안 맞는다(누락/중복).</li>
     * </ul>
     *
     * <p><b>왜 레거시 {@code chunk(size, txManager)} 빌더를 쓰나 (Batch 6 핵심 메모):</b>
     * Batch 6 의 신형 {@code chunk(size)}({@code ChunkOrientedStep}) 에 {@code taskExecutor} 를 주면,
     * 읽기는 <b>단일 스레드</b>로 하고 <b>가공(Processor)만</b> 병렬화한다(=선행학습 자료의 'Async 카드').
     * 즉 신형 API 로는 "여러 스레드가 Reader 하나를 공유" 하는 전통적 함정이 <b>구조적으로 재현되지 않는다.</b>
     * 그래서 여기서는 여러 스레드가 청크를 통째로 도는 <b>레거시 Multi-threaded Step</b>
     * ({@code chunk(size, txManager)} + {@code TaskExecutorRepeatTemplate})을 일부러 사용해
     * Reader 공유 경쟁을 그대로 보여준다. (이 방식은 Batch 6 에서 비권장으로, 바로 이런 위험 때문이다.)
     *
     * @param threads    동시 실행 스레드 수.
     * @param safeReader true 면 Step2 의 표준(thread-safe) Reader 를 써서 <b>데이터는 안 꼬이고</b>
     *                   오직 커넥션 풀 고갈만 보이게 한다 → <b>[Step4-1]</b> 풀 튜닝 데모용.
     *                   false 면 {@link UnsafeSharedOrderReader} 로 <b>데이터 꼬임</b>을 재현한다 → <b>[Step4-2]</b>.
     */
    @SuppressWarnings("removal") // 레거시 Multi-threaded Step 빌더를 '의도적으로' 쓴다(함정 재현). Batch 6 에서 비권장.
    public Job multiThreadedJob(int threads, boolean safeReader) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("mt-worker-");
        executor.setConcurrencyLimit(threads);

        ItemReader<Order> reader = safeReader ? settlementOrderReader : unsafeSharedOrderReader;

        Step step = new StepBuilder("settlementMultiThreadedStep", jobRepository)
                .<Order, Settlement>chunk(chunkSize, transactionManager) // 레거시 빌더 = 진짜 멀티스레드 Step(동시 read)
                .reader(reader)
                .processor(settlementProcessor)
                .writer(settlementWriter)
                .taskExecutor(executor) // ← 한 줄로 멀티스레드. 쉬워 보여서 더 위험하다
                .build();

        return new JobBuilder("settlementMultiThreadedJob", jobRepository)
                .start(step)
                .build();
    }

    /**
     * [Step4-3] 정답 — Partitioning.
     *
     * <p>마스터({@link OrderRangePartitioner})가 id 범위를 {@code gridSize} 개로 쪼개고,
     * 워커 Step({@code settlementWorkerStep})이 파티션마다 <b>전용 Reader</b>로 자기 범위만 처리한다.
     * 워커 Step 실행들이 {@code taskExecutor} 위에서 병렬로 돈다.
     */
    public Job partitionedJob(int gridSize) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("part-worker-");
        executor.setConcurrencyLimit(gridSize);

        Step masterStep = new StepBuilder("settlementPartitionedStep", jobRepository)
                .partitioner("settlementWorkerStep", new OrderRangePartitioner(jdbcTemplate))
                .step(settlementWorkerStep)
                .gridSize(gridSize)
                .taskExecutor(executor)
                .build();

        return new JobBuilder("settlementPartitionedJob", jobRepository)
                .start(masterStep)
                .build();
    }
}
