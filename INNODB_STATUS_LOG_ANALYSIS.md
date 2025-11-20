# InnoDB Status Log 분석

## 기본 정보

```
출력 시간: 2025-11-20 00:58:40
평균 계산 기간: 최근 11초
MySQL 프로세스 ID: 1
```

---

## BACKGROUND THREAD

```
srv_master_thread loops: 63 srv_active, 0 srv_shutdown, 38 srv_idle
srv_master_thread log flush and writes: 0
```

### 설명
- srv_active: 63번 활성 루프 실행
- srv_shutdown: 0번 (종료 프로세스 없음)
- srv_idle: 38번 유휴 상태
- 상태: 정상 작동 중

---

## SEMAPHORES

```
OS WAIT ARRAY INFO: reservation count 2391
OS WAIT ARRAY INFO: signal count 2261
RW-shared spins 0, rounds 0, OS waits 0
RW-excl spins 0, rounds 0, OS waits 0
RW-sx spins 0, rounds 0, OS waits 0
Spin rounds per wait: 0.00 RW-shared, 0.00 RW-excl, 0.00 RW-sx
```

### 설명
- reservation count: 2,391번 대기 예약 발생
- signal count: 2,261번 대기 해제 신호
- RW-shared/excl/sx spins: 0 (스핀락 대기 없음)
- Spin rounds per wait: 0.00 (평균 스핀 횟수)
- 해석: OS 레벨 대기는 발생했으나 스핀락 없이 빠르게 처리됨

---

## LATEST DETECTED DEADLOCK

```
발생 시간: 2025-11-20 00:58:05
```

### Transaction 1

```
TRANSACTION 411283, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
MySQL thread id 14, OS thread handle 281472970252032, query id 106772
```

#### 설명
- 트랜잭션 ID: 411283
- 활성 시간: 0초 (방금 시작)
- 사용 중인 테이블: 1개
- 잠긴 테이블: 1개
- 락 구조체: 7개
- Row 락: 3개
- Undo 로그 엔트리: 1개
- MySQL 스레드 ID: 14
- OS 스레드 핸들: 281472970252032
- 쿼리 ID: 106772

#### 실행 쿼리

```sql
update community_posts
set user_id=9999,
    category='GENERAL',
    comment_count=2791,
    content='이것은 k6 부하 테스트를 위한 샘플 게시글입니다.',
    is_deleted=0,
    is_modified=0,
    like_count=34,
    modified_at=null,
    title='부하 테스트용 게시글 1',
    view_count=10383
where post_id=1
```

#### 보유 중인 락

```
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411283 lock mode S locks rec but not gap
```

##### 설명
- Space ID: 77 (테이블스페이스 식별자)
- Page Number: 6 (데이터 페이지 번호)
- Index: PRIMARY KEY
- Lock Mode: S (Shared Lock)
- Lock Scope: rec but not gap (레코드 락만, Gap 락 아님)

##### 레코드 데이터

```
Record lock, heap no 2 PHYSICAL RECORD: n_fields 15; compact format; info bits 0
 0: post_id = 1                    (len 8; hex 8000000000000001)
 1: transaction_id                 (len 6; hex 000000064687)
 2: rollback_pointer              (len 7; hex 010000008a07f2)
 3: user_id = 9999                (len 8; hex 800000000000270f)
 4: title (한글, 63바이트)         (len 30; hex c3abc2b6...)
 5: content (한글, 135바이트)      (len 30; hex c3acc29d...)
 6: category = 'GENERAL'          (len 7; hex 47454e4552414c)
 7: view_count = 10382            (len 4; hex 8000288e)
 8: like_count = 33               (len 4; hex 80000021)
 9: comment_count = 2791          (len 4; hex 80000ae7)
10: created_at                    (len 4; hex 691d70b5)
11: updated_at                    (len 4; hex 691e679d)
12: is_modified = 0               (len 1; hex 80)
13: modified_at = NULL            (SQL NULL)
14: is_deleted = 0                (len 1; hex 80)
```

#### 대기 중인 락

```
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411283 lock_mode X locks rec but not gap waiting
```

##### 설명
- Lock Mode: X (Exclusive Lock)
- Status: waiting (대기 중)
- 동일한 레코드 (post_id = 1)에 대한 X Lock 요청 중

---

### Transaction 2

```
TRANSACTION 411294, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
MySQL thread id 17, OS thread handle 281472966897408, query id 106771
```

#### 설명
- 트랜잭션 ID: 411294
- 활성 시간: 0초
- 사용/잠긴 테이블: 1개
- 락 구조체: 7개
- Row 락: 3개
- MySQL 스레드 ID: 17
- 쿼리 ID: 106771

#### 실행 쿼리

```sql
update community_posts
set user_id=9999,
    category='GENERAL',
    comment_count=2792,
    content='이것은 k6 부하 테스트를 위한 샘플 게시글입니다.',
    is_deleted=0,
    is_modified=0,
    like_count=33,
    modified_at=null,
    title='부하 테스트용 게시글 1',
    view_count=10380
where post_id=1
```

#### 보유 중인 락

```
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411294 lock mode S locks rec but not gap
```

##### 설명
- Lock Mode: S (Shared Lock)
- 동일한 레코드 (post_id = 1)에 S Lock 보유

#### 대기 중인 락

```
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411294 lock_mode X locks rec but not gap waiting
```

##### 설명
- Lock Mode: X (Exclusive Lock)
- Status: waiting (대기 중)

### 데드락 해결

```
*** WE ROLL BACK TRANSACTION (2)
```

#### 설명
- MySQL이 데드락을 감지
- Transaction 2 (ID: 411294)를 롤백
- Transaction 1은 계속 진행

#### 데드락 패턴

```
상황:
- Transaction 1: S Lock 보유 → X Lock 대기
- Transaction 2: S Lock 보유 → X Lock 대기

결과:
- 서로가 서로의 S Lock 때문에 X Lock을 획득하지 못함
- 순환 대기 발생
- MySQL이 자동으로 하나를 롤백하여 해결
```

---

## TRANSACTIONS

```
Trx id counter 411518
Purge done for trx's n:o < 411518 undo n:o < 0 state: running but idle
History list length 43
```

### 설명
- 다음 트랜잭션 ID: 411,518
- Purge 상태: 411,518 이전 트랜잭션까지 정리 완료
- History list: 43개의 정리 대기 레코드
- Purge 스레드: 실행 중이지만 유휴 상태

### 현재 세션 트랜잭션

```
13개의 세션이 "not started" 상태
0 lock struct(s), heap size 1128, 0 row lock(s)
```

#### 설명
- 모든 세션이 유휴 상태
- 활성 트랜잭션 없음
- 락 없음

---

## FILE I/O

```
I/O thread 0 state: waiting for completed aio requests (insert buffer thread)
I/O thread 1-4 state: waiting for completed aio requests (read thread)
I/O thread 5-8 state: waiting for completed aio requests (write thread)
```

### 설명
- 총 9개의 I/O 스레드
- 1개 insert buffer 스레드
- 4개 read 스레드
- 4개 write 스레드
- 모두 대기 상태 (요청 없음)

### I/O 통계

```
Pending normal aio reads: [0, 0, 0, 0]
Pending aio writes: [0, 0, 0, 0]
Pending flushes (fsync) log: 0
Pending buffer pool: 0
```

#### 설명
- 대기 중인 비동기 읽기: 없음
- 대기 중인 비동기 쓰기: 없음
- 대기 중인 fsync: 없음
- 대기 중인 buffer pool flush: 없음

### 누적 통계

```
2121 OS file reads
50128 OS file writes
31681 OS fsyncs
```

#### 설명
- 파일 읽기: 2,121회
- 파일 쓰기: 50,128회
- fsync 호출: 31,681회
- 쓰기가 많은 이유: 부하 테스트로 인한 대량 INSERT/UPDATE

### 현재 속도

```
0.00 reads/s
0 avg bytes/read
0.00 writes/s
0.00 fsyncs/s
```

#### 설명
- 현재 I/O 없음
- 부하 테스트 종료된 상태

---

## INSERT BUFFER AND ADAPTIVE HASH INDEX

```
Ibuf: size 1, free list len 0, seg size 2, 6 merges
merged operations:
 insert 17, delete mark 0, delete 0
discarded operations:
 insert 0, delete mark 0, delete 0
```

### 설명
- Insert Buffer 크기: 1페이지
- Free list 길이: 0
- Segment 크기: 2페이지
- 병합 횟수: 6회
- 병합된 insert: 17개
- 병합된 delete mark: 0개
- 폐기된 작업: 없음

### Adaptive Hash Index

```
Hash table size 34679
총 8개의 해시 테이블
node heap 총 11 buffer(s)
```

#### 설명
- 해시 테이블 크기: 34,679 슬롯
- 8개의 파티션으로 분할
- 사용 중인 버퍼: 11개

### 검색 통계

```
0.00 hash searches/s
0.00 non-hash searches/s
```

#### 설명
- 현재 검색 없음

---

## LOG

```
Log sequence number          169066688
Log buffer assigned up to    169066688
Log buffer completed up to   169066688
Log written up to            169066688
Log flushed up to            169066688
Added dirty pages up to      169066688
Pages flushed up to          169066688
Last checkpoint at           169066688
```

### 설명
- 모든 로그 시퀀스 번호가 동일
- 의미: 모든 트랜잭션이 완전히 디스크에 기록됨
- Checkpoint와 현재 위치가 동일: 복구 필요 없음
- 상태: 완전히 동기화됨

### Redo Log 파일

```
Log minimum file id is 48
Log maximum file id is 51
```

#### 설명
- 사용 중인 Redo Log 파일 범위: 48~51
- 총 4개의 로그 파일 순환 사용 중

### Log I/O

```
47510 log i/o's done
0.00 log i/o's/second
```

#### 설명
- 누적 로그 I/O: 47,510회
- 현재 속도: 0 (활동 없음)

---

## BUFFER POOL AND MEMORY

```
Total large memory allocated 0
Dictionary memory allocated 762215
```

### 설명
- 대용량 메모리 할당: 0바이트
- Dictionary 메모리: 762,215바이트 (약 744KB)
- Dictionary: 테이블/인덱스 메타데이터 저장

### Buffer Pool 설정

```
Buffer pool size   8192
```

#### 설명
- 크기: 8,192 페이지
- 계산: 8,192 × 16KB = 128MB
- 비고: 기본값, 운영 환경에서는 RAM의 70-80% 권장

### Buffer Pool 사용 현황

```
Free buffers       6465
Database pages     1716
Old database pages 650
Modified db pages  0
```

#### 설명
- Free: 6,465 페이지 (78.9%)
- 사용 중: 1,716 페이지 (21.1%)
- Old pages: 650 페이지
- Dirty pages: 0 (모든 변경사항 디스크 기록 완료)

### I/O 대기

```
Pending reads      0
Pending writes: LRU 0, flush list 0, single page 0
```

#### 설명
- 대기 중인 읽기: 없음
- 대기 중인 쓰기: 없음
- I/O 병목 없음

### LRU 알고리즘

```
Pages made young 3
Pages made not young 0
0.00 youngs/s
0.00 non-youngs/s
```

#### 설명
- young: 3페이지가 최근 사용으로 이동
- not young: 0페이지
- 현재 속도: 0 (활동 없음)

### 페이지 I/O 통계

```
Pages read 1450
Pages created 266
Pages written 2075
```

#### 설명
- 읽기: 1,450 페이지
- 생성: 266 페이지
- 쓰기: 2,075 페이지

### 현재 속도

```
0.00 reads/s
0.00 creates/s
0.00 writes/s
```

#### 설명
- 현재 활동 없음

### Read-ahead

```
Pages read ahead 0.00/s
evicted without access 0.00/s
Random read ahead 0.00/s
```

#### 설명
- Read-ahead: 미리 읽기 없음
- Evicted: 캐시에서 제거 없음
- Random read-ahead: 없음

### LRU 리스트

```
LRU len: 1716
unzip_LRU len: 0
I/O sum[0]:cur[0]
unzip sum[0]:cur[0]
```

#### 설명
- LRU 리스트 길이: 1,716 페이지
- 압축 해제 리스트: 0 (압축된 페이지 없음)

---

## ROW OPERATIONS

```
0 queries inside InnoDB, 0 queries in queue
0 read views open inside InnoDB
```

### 설명
- 실행 중인 쿼리: 0개
- 대기 중인 쿼리: 0개
- Read View: 0개 (활성 트랜잭션 없음)

### MySQL 프로세스

```
Process ID=1
Main thread ID=281473005186816
state=sleeping
```

#### 설명
- MySQL 메인 스레드 상태: sleeping (유휴)

### Row 작업 통계 (누적)

```
Number of rows inserted 6959
Number of rows updated 13265
Number of rows deleted 1482
Number of rows read 103585
```

#### 설명
- INSERT: 6,959건
- UPDATE: 13,265건 (가장 많음)
- DELETE: 1,482건
- READ: 103,585건
- 비고: MySQL 시작 후 누적 통계

### 현재 속도

```
0.00 inserts/s
0.00 updates/s
0.00 deletes/s
0.00 reads/s
```

#### 설명
- 현재 작업 없음

### 시스템 Row 통계

```
Number of system rows inserted 8
Number of system rows updated 331
Number of system rows deleted 8
Number of system rows read 7279
```

#### 설명
- 시스템 테이블 통계 (메타데이터)
- UPDATE 331건: 통계 정보 업데이트

### 현재 속도

```
0.00 inserts/s
0.00 updates/s
0.00 deletes/s
0.00 reads/s
```

#### 설명
- 현재 시스템 작업 없음

---

## 요약

### 데드락 정보
- 발생 시간: 2025-11-20 00:58:05
- 패턴: Lock Upgrade Deadlock (S Lock → X Lock 업그레이드 충돌)
- 해결: Transaction 2 롤백

### 현재 상태
- 활성 트랜잭션: 없음
- 대기 중인 쿼리: 없음
- I/O 병목: 없음
- Dirty pages: 0 (완전 동기화)

### Buffer Pool
- 크기: 128MB
- 사용률: 21.1%
- 여유: 충분

### 성능 지표
- 모든 초당 통계: 0 (활동 없음)
- 누적 UPDATE: 13,265건 (가장 많음)
