# 정산(Settlement) 배치 세션 - 진행 가이드

> 기준 브랜치: `lectures/settlement/step1`
> 기존 주문/결제 모놀리식 위에, 정산을 **Spring Batch** 로 만들어 가는 1일 세션입니다.

이 문서는 **강사용 로드맵**입니다. step1 에는 "출발점"이 되는 코드만 들어 있고,
배치 Job/Step 은 세션 중 라이브 코딩으로 채워 갑니다.

---

## 0. step1 에 미리 세팅된 것 (출발점)

| 영역 | 위치 | 설명 |
|---|---|---|
| 정산 엔티티 | `settlement/domain/Settlement.java` | 결과 테이블. **1원 무결성**(수수료+지급액=주문금액)을 `verifyIntegrity()` 로 강제. `order_id` 유니크 = 멱등성 키 |
| 정산 상태 | `settlement/domain/SettlementStatus.java` | PENDING / COMPLETED / FAILED |
| 리포지토리 | `settlement/domain` + `settlement/infrastructure` | `existsByOrderId` 등 멱등성 체크용 메서드 포함 |
| **안티패턴** 서비스 | `settlement/application/NaiveSettlementService.java` | `settleAll()`=findAll 전량 적재(즉시 OOM), `settleUpTo(limit)`=조금씩 쌓으며 정산(추세 관찰). 둘 다 시간·피크힙 리포트 |
| 힙 모니터 | `settlement/support/HeapMonitor.java` | 작업 중 힙 사용량을 0.5초마다 `[mem]` 막대로 로그 → 메모리 차오르는 과정 실시간 시각화 |
| 트리거 API | `settlement/presentation/SettlementController.java` | `POST /settlements/naive[?limit=N]`, `GET /settlements/status`(건수+힙), `DELETE /settlements` |
| 대용량 시더 | `settlement/support/SettlementDataSeeder.java` | JdbcTemplate 배치 INSERT 로 주문/결제 N건 적재 (`settlement.seed.enabled=true`) |
| 배치 인프라 | `build.gradle`, `application.properties` | `spring-boot-starter-batch`, JobRepository 스키마 자동 생성, Job 자동실행 OFF |
| 요청 모음 | `settlement.http` | 실습 엔드포인트 호출 모음 |

> 데이터 모델 메모: 시더는 `payments.id == orders.id == orders.payment_id` 로 1:1 매핑합니다.
> 그래서 조인 실습은 `orders ⨝ payments ON orders.payment_id = payments.id` 형태가 됩니다.
> 금액은 일부러 3%로 나누어 떨어지지 않게 만들어 반올림(1원) 이슈가 드러나도록 했습니다.

---

## 1. [09:00~13:00] 개념 + 기초 파이프라인

### (1) 정산 도메인의 특수성 - 1원의 무결성
- `Settlement.of(...)` 를 열어 설명: **수수료만 반올림(HALF_UP)** 하고 **지급액은 `주문금액 - 수수료`(나머지)** 로 계산.
- 만약 지급액도 따로 반올림하면? → `feeAmount + payoutAmount != orderAmount` → `verifyIntegrity()` 에서 즉시 실패.
- 토크 포인트: "정산은 1원이 틀리면 사고다. 어디서 반올림하고 어디서 나머지를 취하느냐가 설계다."

### (2) 배치가 필요한 이유 - 직접 보여주기 (Step1 핵심 데모)

목표: 수강생이 **메모리가 차오르다 터지는 과정**과 **무엇이 느린지**를 눈으로 보게 한다.

#### 준비 — main() 을 Run/Profile 로, 힙을 좁혀서
IDE Run Configuration → VM options:
```
-Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./oom.hprof -Xlog:gc*:file=gc.log:tags,uptime
```
Program arguments(또는 application.properties):
```
--settlement.seed.enabled=true --settlement.seed.count=1000000 --spring.jpa.show-sql=false --logging.level.org.hibernate.orm.jdbc.bind=off
```
> `bootRun` 은 VM 옵션이 앱 JVM 에 안 먹을 수 있으니 **main() 직접 실행**을 권장.
> 부팅 로그의 `[SEED] 완료 ... 소요시간=...ms` → 첫 번째 "오래 걸리는 작업"(대량 적재) 확인.

#### 데모 A — "절벽으로 걸어가기" (느려지는 추세 + 메모리 증가)
```http
GET  http://localhost:8080/settlements/status                 # 시작 힙/건수
POST http://localhost:8080/settlements/naive?limit=100000     # 10만
DELETE http://localhost:8080/settlements
POST http://localhost:8080/settlements/naive?limit=300000     # 30만
DELETE http://localhost:8080/settlements
POST http://localhost:8080/settlements/naive?limit=500000     # 50만
```
- 응답의 `elapsedMs` 와 `peakHeapMb` 가 limit 에 비례해 **쭉 증가**하는 걸 표로 적어가며 보여준다.
- 콘솔의 `[mem:naive-climb] used=…MB / max=1024MB (NN%) [######----]` 막대가 호출마다 더 차오른다.
- 토크 포인트: "데이터가 늘면 시간도 메모리도 **선형으로** 증가한다. 이대로면 끝은 절벽이다."

#### 데모 B — "한 줄의 함정" (OOM)
```http
DELETE http://localhost:8080/settlements
POST   http://localhost:8080/settlements/naive                # limit 없음 = findAll 전량
```
- `[mem:naive-findAll]` 막대가 `max` 에 붙는 순간 `OutOfMemoryError` → `./oom.hprof` 생성.
- 토크 포인트: "이 **한 줄**(`findAll()`)이 100만 엔티티를 통째로 올린다. 100만을 다 못 읽고 죽는다."

#### 데모 C — 힙 덤프 분석 (왜/무엇이 터졌나)
`oom.hprof` 를 IntelliJ Ultimate 로 열고:
- **Count 정렬**: `Order`, `Money`, `EntityEntryImpl` 가 수십만~수백만 개
  - `EntityEntryImpl 수 ≈ Order 수` → 엔티티마다 영속성 컨텍스트 관리비용이 1:1로 붙음
  - `Money 수 ≈ Order×3×2` → 변경감지 **스냅샷 때문에 2배**로 든다
- **Retained 정렬 / Biggest Objects**: `StatefulPersistenceContext` 1개가 수백MB를 붙들어 GC 불가
- **Summary 탭**: GC 후 남은 live 총량(=줄일 수 없는 알맹이) 확인
- `gc.log`: Full GC 가 반복되는데 회수율 ≈ 0 → `GC overhead limit exceeded` 의 정체
- 주의: 인메모리 H2 라서 **시드 데이터(약 330MB)가 힙에 상주** → findAll 에 주어진 공간은 1GB 가 아니라 ~670MB. (더 깔끔히 분리하려면 아래 파일 H2 옵션)

#### (선택) 파일 기반 H2 로 "findAll 단독 범인" 부각
```
--spring.datasource.url=jdbc:h2:file:./build/oomdemo;DB_CLOSE_DELAY=-1
```
DB 데이터가 디스크로 빠져 힙에서 사라지므로, 힙 덤프에 **Hibernate 적재분만** 도드라진다.

#### 마무리 → 다음(Chunk)으로 연결
- "끊어서(Chunk) 1,000건만 올리고 → 쓰고 → `clear()` 로 비우면, `StatefulPersistenceContext` 가 절대 안 커진다."
- 그래서 (3) Spring Batch Chunk 지향 처리로 넘어간다.

### (3) 스프링 배치 기초 - Chunk 지향 처리 (라이브 코딩)
`settlement/batch/` 패키지를 새로 만들며 진행:
- **ItemReader**: `JpaPagingItemReader` 또는 `JdbcPagingItemReader` 로 주문을 페이지 단위로 읽기 (메모리 안전).
- **ItemProcessor**: 주문 → `Settlement` 변환 (1원 무결성 계산 재사용).
- **ItemWriter**: `JpaItemWriter` / `JdbcBatchItemWriter` 로 정산 테이블에 적재.
- chunk size 를 바꿔 가며 메모리/속도 트레이드오프 관찰.
- 실행 엔드포인트를 `SettlementController` 에 추가 (`POST /settlements/batch`).

### (4) 멱등성과 재시작 (Restartability) (라이브 코딩)
- 시나리오: 50% 지점에서 강제 예외 → Job 실패.
- 다시 실행하면? **이미 정산된 50%는 건너뛰고 나머지만** 처리해야 한다.
- 구현 축:
  - `order_id` 유니크 제약(이미 있음) + Processor 에서 `existsByOrderId` 로 스킵, 또는
  - Step 의 `lastProcessedId` 를 `ExecutionContext` 에 저장해 이어서 읽기.
  - 같은 `JobParameters` 로 재실행 → 실패한 Step 부터 재개되는 Spring Batch 기본 동작 보여주기.
- naive 방식은 재실행하면 유니크 제약으로 그냥 터진다 → 대비 효과.

---

## 2. [14:00~18:00] 라이브 세션 - 성능 한계 돌파

### (1) 멀티스레드 & 파티셔닝
- **Multi-threaded Step**: `taskExecutor` 부여 → 스레드 수를 올리며 처리량 변화 관찰.
- **커넥션 풀 터뜨리기**: 스레드 수 > HikariCP `maximum-pool-size` → 커넥션 대기/타임아웃 재현 → 풀 튜닝.
  ```properties
  spring.datasource.hikari.maximum-pool-size=20
  spring.datasource.hikari.connection-timeout=3000
  ```
- **AsyncItemProcessor / AsyncItemWriter**: Processor 를 비동기로.
- **Partitioning**: `order_id` 범위로 파티션을 나눠 병렬 Step 실행 (마스터-워커).

### (2) 대용량 조인 최적화 & DB 부하 줄이기
- 주문-결제(-정산) 조인 시 성능 저하 → **Driving Table** 선택, 인덱스 활용.
- **Cursor vs Paging** 선택 기준:
  - Paging: 매 페이지 `OFFSET` 비용 / 단순.
  - Cursor: 커넥션 유지하며 스트리밍 / 멀티스레드와는 상성 주의.
- **Bulk Insert**: `JdbcBatchItemWriter` + `rewriteBatchedStatements`(MySQL) 류로 INSERT 묶기.
- **인덱스 전략**: 조인/조회 키(`payment_id`, `order_id`)에 인덱스, 정산 테이블 유니크 인덱스.

### (3) 요약 & Q&A

---

## 부록 - 자주 쓰는 명령

```bash
# 일반 기동 (시드 없음)
./gradlew bootRun

# 소량 시드로 빠르게 흐름만 확인
./gradlew bootRun --args='--settlement.seed.enabled=true --settlement.seed.count=2000 --spring.jpa.show-sql=false'

# 정산 결과 초기화 (멱등성/재시작 데모 반복용)
curl -X DELETE http://localhost:8080/settlements

# H2 콘솔: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:tangled)
```

> 참고: 대량 시드/배치 실행 시에는 `--spring.jpa.show-sql=false --logging.level.org.hibernate.orm.jdbc.bind=off`
> 로 로그 폭주를 막으세요.
