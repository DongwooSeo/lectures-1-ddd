package com.growmighty.lectures.firstday.tangledmonolith.settlement.batch;

import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * [Step4-2] <b>일부러 thread-safe 하지 않게 만든</b> 페이징 Reader — 멀티스레드 함정 재현용.
 *
 * <p>2부 선행학습 자료의 그림을 그대로 코드로 옮긴 것이다.
 * <pre>
 *        ┌──────────── 같은 Reader 하나 (cursor/page 공유) ───────────┐
 * 스레드A → read() ─┐                                  같은 위치를
 * 스레드B → read() ─┘  동시에 들어옴 → cursor 가 꼬임 → 둘 다 읽거나(중복)
 *                                                     하나가 건너뜀(누락)
 * </pre>
 *
 * <p><b>왜 직접 만들었나 (중요한 버전 메모):</b> Spring Batch 6 의 표준
 * {@code JpaPagingItemReader.read()} 는 내부적으로 {@code Lock} 으로 보호되도록 바뀌어, 표준
 * Reader 만으로는 인덱스 꼬임이 잘 재현되지 않는다(선행학습 자료의 "{@code SynchronizedItemStreamReader}
 * 반창고" 가 표준 Reader 안에 이미 박혀 있는 셈 — 읽기를 직렬화해 안전하지만 근본 처방은 아니다).
 * 그래서 "공유 상태를 여러 스레드가 경쟁하면 데이터가 꼬인다" 는 본질을 보여주기 위해
 * <b>동기화를 일부러 뺀</b> 이 Reader 를 쓴다. (운영 코드에 두면 안 되는, 데모 전용 안티패턴)
 *
 * <p><b>이 코드베이스에서 실제로 보이는 증상:</b> Multi-threaded Step 으로 돌리면 공유 cursor 경쟁으로
 * <b>같은 주문이 두 스레드에 동시에</b> 들어가, Step3 에서 건 {@code order_id} UNIQUE 제약이
 * '이중정산'을 막아 <b>Job 이 FAILED</b> 된다 — 그것도 매번 다른 지점에서, 커밋된 건수도 들쭉날쭉한
 * <b>비결정적</b> 실패다. (만약 UNIQUE 가 없었다면 조용한 이중정산·누락 '사고'가 났을 것이다.)
 * 어느 쪽이든 원인은 하나 — 공유 Reader 경쟁. → 해법은 파티셔닝.
 *
 * <p>{@code @StepScope} 라 Step 실행마다 인스턴스가 <b>하나</b> 만들어지고, 그 하나를
 * 레거시 Multi-threaded Step 의 모든 워커 스레드가 공유한다 — 공유가 곧 함정이다.
 * (Batch 6 신형 {@code chunk(size)} 빌더는 읽기를 단일 스레드로 직렬화하므로 이 함정이 재현되지 않는다.
 * 그래서 {@link SettlementParallelJobFactory#multiThreadedJob(int)} 은 레거시 빌더를 일부러 쓴다.)
 *
 * <p>JPA 조회 자체는 페이지마다 독립 {@link EntityManager} 를 열어 안전하게 한다.
 * 꼬이는 건 오직 {@code cursor / pageNumber / page} 라는 <b>공유 가변 상태</b>뿐이다.
 *
 * @see SettlementParallelJobFactory#multiThreadedJob(int)
 * @see OrderRangePartitioner 구조적 해법(파티셔닝)은 이 공유 자체를 없앤다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class UnsafeSharedOrderReader implements ItemReader<Order> {

    private final EntityManagerFactory emf;

    @Value("${settlement.batch.chunk-size:1000}")
    private int pageSize;

    // ⚠️ 동기화 없는 공유 상태 — 여러 스레드가 동시에 만지며 경쟁한다 (의도된 버그)
    private volatile List<Order> page = List.of();
    private int cursor = 0;
    private int pageNumber = 0;
    private volatile boolean exhausted = false;

    /**
     * read-modify-write 사이의 '틈'을 일부러 넓혀 경쟁을 증폭하는 데모용 지연(ns).
     *
     * <p>전통적 Multi-threaded Step(여러 스레드가 동시에 read)에서는 <b>0 으로 둬도 거의 매번</b>
     * 재현된다(같은 주문이 두 스레드에 들어가 order_id UNIQUE 위반 → Job FAILED). 코어가 적거나
     * 운 좋게 안 터지는 환경이면 값을 키워(예: 1_000) 틈을 벌리면 확실히 터진다.
     */
    private static final long RACE_WINDOW_NANOS = 0;

    @Override
    public Order read() {
        if (exhausted) {
            return null;
        }

        // 현재 페이지를 다 읽었으면 다음 페이지 적재.
        // 두 스레드가 동시에 들어오면 같은 페이지를 중복 적재하거나 pageNumber 가 두 번 올라 한 페이지를 통째로 건너뛴다(누락).
        if (cursor >= page.size()) {
            int pageToLoad = pageNumber;
            widenRaceWindow();
            List<Order> next = loadPage(pageToLoad);
            pageNumber = pageToLoad + 1;
            cursor = 0;
            page = next;
            if (next.isEmpty()) {
                exhausted = true;
                return null;
            }
        }

        // 경쟁의 핵심: cursor 를 '읽고 → (틈) → 쓰기' 로 쪼갠다. 두 스레드가 같은 index 를 받으면
        // 같은 주문을 두 번 정산하려다 UNIQUE 위반(이중정산 시도), 증가가 묻히면 누락. 동기화가 없어 둘 다 일어난다.
        int index = cursor;
        widenRaceWindow();
        cursor = index + 1;

        List<Order> snapshot = page;        // 페이지 교체 도중의 IndexOutOfBounds 만 회피 (값 경쟁은 그대로 둔다)
        if (index < 0 || index >= snapshot.size()) {
            return read();
        }
        return snapshot.get(index);
    }

    /** read-modify-write 사이를 일부러 벌려 경쟁을 눈에 보이게 만든다(데모 전용 증폭 장치). */
    private static void widenRaceWindow() {
        if (RACE_WINDOW_NANOS > 0) {
            LockSupport.parkNanos(RACE_WINDOW_NANOS);
        }
    }

    /** 페이지마다 독립 EntityManager 로 안전하게 조회 (공유 상태만 경쟁하도록). */
    private List<Order> loadPage(int pageNo) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT o FROM Order o WHERE o.status = :status ORDER BY o.id ASC", Order.class)
                    .setParameter("status", OrderStatus.PAID)
                    .setFirstResult(pageNo * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
