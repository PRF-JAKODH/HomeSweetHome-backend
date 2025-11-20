# InnoDB 상태 로그 분석 (상세 한글 주석)

## 📌 요약

```
데드락 발생 시간: 2025-11-20 00:58:05
원인: JPA Dirty Checking (전체 컬럼 UPDATE)
상태: 애플리케이션 재시작 필요! (예전 코드 실행 중)
```

---

## 🔍 전체 로그 분석

```sql
=====================================
2025-11-20 00:58:40 281472964660992 INNODB MONITOR OUTPUT
=====================================
Per second averages calculated from the last 11 seconds
# 마지막 11초 동안의 평균 통계

-----------------
BACKGROUND THREAD
-----------------
srv_master_thread loops: 63 srv_active, 0 srv_shutdown, 38 srv_idle
srv_master_thread log flush and writes: 0

# 백그라운드 스레드 상태
# - srv_active: 63번 활성 루프 실행
# - srv_shutdown: 0번 (종료 중 아님)
# - srv_idle: 38번 유휴 상태
# ✅ 정상 작동 중

----------
SEMAPHORES
----------
OS WAIT ARRAY INFO: reservation count 2391
OS WAIT ARRAY INFO: signal count 2261
RW-shared spins 0, rounds 0, OS waits 0
RW-excl spins 0, rounds 0, OS waits 0
RW-sx spins 0, rounds 0, OS waits 0
Spin rounds per wait: 0.00 RW-shared, 0.00 RW-excl, 0.00 RW-sx

# 세마포어 (동기화 메커니즘) 통계
# - reservation count: 2,391번 대기 발생
# - signal count: 2,261번 신호 발생
# - RW-shared/excl/sx spins: 0 (스핀락 대기 없음)
#
# 💡 의미:
# - OS 레벨 대기는 많지만 (2,391번)
# - 스핀락 대기는 없음 (빠르게 해결됨)
# ✅ 락 경합은 심하지 않음

------------------------
LATEST DETECTED DEADLOCK  # 🚨 가장 최근 데드락 발견!
------------------------
2025-11-20 00:58:05 281473161477888
# 데드락 발생 시간: 2025-11-20 00:58:05 (35초 전)

*** (1) TRANSACTION:  # 트랜잭션 1
TRANSACTION 411283, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
# - 트랜잭션 ID: 411283
# - 활성 시간: 0초 (방금 시작)
# - 테이블 1개 사용 중, 1개 락 걸림

LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
# - 7개의 락 구조체
# - 3개의 row 락
# - 1개의 undo 로그 엔트리

MySQL thread id 14, OS thread handle 281472970252032, query id 106772 192.168.65.1 user updating
update community_posts
set user_id=9999,
    category='GENERAL',
    comment_count=2791,  # ❌ 전체 컬럼 UPDATE!
    content='이것은 k6 부하 테스트를 위한 샘플 게시글입니다.',
    is_deleted=0,
    is_modified=0,
    like_count=34,
    modified_at=null,
    title='부하 테스트용 게시글 1',
    view_count=10383
where post_id=1

# 💀 문제 발견!
# 이건 JPA Dirty Checking이 생성한 쿼리!
# 모든 컬럼을 업데이트하고 있음 (view_count, like_count, comment_count 모두)
#
# 🎯 우리가 구현한 JPQL Atomic은:
# UPDATE community_posts SET view_count = view_count + 1 WHERE post_id = 1
#
# 결론: 애플리케이션이 아직 재시작 안 됨!

*** (1) HOLDS THE LOCK(S):  # 트랜잭션 1이 가지고 있는 락
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411283 lock mode S locks rec but not gap

# 💡 S-Lock (Shared Lock) 보유
# - PRIMARY KEY 인덱스에 S-Lock
# - "rec but not gap": 레코드만 락 (gap 락 아님)
#
# 왜 S-Lock을 먼저 걸까?
# → JPA가 SELECT ... FOR UPDATE를 실행하면
#    MySQL REPEATABLE-READ 격리 수준에서
#    먼저 S-Lock을 걸고 → 나중에 X-Lock으로 업그레이드

Record lock, heap no 2 PHYSICAL RECORD: n_fields 15; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;  # post_id = 1
 1: len 6; hex 000000064687; asc     F ;;        # 트랜잭션 ID
 2: len 7; hex 010000008a07f2; asc        ;;     # 롤백 포인터
 3: len 8; hex 800000000000270f; asc       ' ;;  # user_id = 9999
 4: len 30; hex c3abc2b6e282acc3ade280a2cb9c...  # title (한글)
 5: len 30; hex c3acc29dc2b4c3aak2b2c692...      # content (한글)
 6: len 7; hex 47454e4552414c; asc GENERAL;;     # category = 'GENERAL'
 7: len 4; hex 8000288e; asc   ( ;;              # view_count = 10,382
 8: len 4; hex 80000021; asc    !;;              # like_count = 33
 9: len 4; hex 80000ae7; asc     ;;              # comment_count = 2,791
 10: len 4; hex 691d70b5; asc i p ;;             # created_at
 11: len 4; hex 691e679d; asc i g ;;             # updated_at
 12: len 1; hex 80; asc  ;;                      # is_modified = 0
 13: SQL NULL;                                    # modified_at = NULL
 14: len 1; hex 80; asc  ;;                      # is_deleted = 0

# 💡 post_id=1 레코드의 실제 데이터
# - view_count: 10,382
# - like_count: 33
# - comment_count: 2,791


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:  # 트랜잭션 1이 기다리는 락
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411283 lock_mode X locks rec but not gap waiting

# 💀 X-Lock (Exclusive Lock) 대기 중!
#
# 데드락 패턴 발견:
# 1. 트랜잭션 1: S-Lock 보유 → X-Lock 요청 (대기)
# 2. 트랜잭션 2: S-Lock 보유 → X-Lock 요청 (대기)
#
# 서로가 서로의 S-Lock 때문에 X-Lock을 못 얻음!
# → 데드락!

Record lock, heap no 2 PHYSICAL RECORD: n_fields 15; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;  # post_id = 1 (같은 레코드!)
 # ... (동일한 레코드)


*** (2) TRANSACTION:  # 트랜잭션 2
TRANSACTION 411294, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
MySQL thread id 17, OS thread handle 281472966897408, query id 106771 192.168.65.1 user updating
update community_posts
set user_id=9999,
    category='GENERAL',
    comment_count=2792,  # ❌ 또 전체 컬럼 UPDATE!
    content='이것은 k6 부하 테스트를 위한 샘플 게시글입니다.',
    is_deleted=0,
    is_modified=0,
    like_count=33,
    modified_at=null,
    title='부하 테스트용 게시글 1',
    view_count=10380
where post_id=1

# 💀 트랜잭션 2도 동일한 패턴!
# - JPA Dirty Checking
# - 모든 컬럼 업데이트
# - 같은 post_id=1


*** (2) HOLDS THE LOCK(S):  # 트랜잭션 2가 가지고 있는 락
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411294 lock mode S locks rec but not gap

# 💡 트랜잭션 2도 S-Lock 보유!
#
# 데드락 상황:
# - 트랜잭션 1: S-Lock 보유, X-Lock 대기
# - 트랜잭션 2: S-Lock 보유, X-Lock 대기
# → 서로 막힘!

Record lock, heap no 2 PHYSICAL RECORD: n_fields 15; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;  # post_id = 1 (같은 레코드!)
 # ... (동일)


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:  # 트랜잭션 2가 기다리는 락
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 411294 lock_mode X locks rec but not gap waiting

# 💀 트랜잭션 2도 X-Lock 대기 중!

Record lock, heap no 2 PHYSICAL RECORD: n_fields 15; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;  # post_id = 1 (같은 레코드!)
 # ... (동일)


*** WE ROLL BACK TRANSACTION (2)  # 🚨 트랜잭션 2 롤백!
# MySQL이 데드락을 감지하고 트랜잭션 2를 롤백시킴
# → 트랜잭션 1은 계속 진행

# 💡 데드락 해결 방법:
# - MySQL은 데드락을 감지하면 자동으로 하나를 롤백
# - 일반적으로 undo 로그가 적은 쪽을 롤백
# - 롤백된 트랜잭션은 에러 반환 (SQL Error 1213)

------------
TRANSACTIONS
------------
Trx id counter 411518
# 다음 트랜잭션 ID: 411,518
# (데드락 이후 많은 트랜잭션이 실행됨)

Purge done for trx's n:o < 411518 undo n:o < 0 state: running but idle
History list length 43
# - Purge: 완료된 트랜잭션 정리 중
# - History list: 43개의 정리 대기 중인 레코드
# ✅ 정상 범위

LIST OF TRANSACTIONS FOR EACH SESSION:
---TRANSACTION 562948546927720, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
# ... (13개의 트랜잭션이 "not started" 상태)

# 💡 현재 활성 트랜잭션 없음
# - 모든 세션이 유휴 상태
# - k6 테스트가 끝났거나 일시 중지됨

--------
FILE I/O
--------
I/O thread 0 state: waiting for completed aio requests (insert buffer thread)
I/O thread 1 state: waiting for completed aio requests (read thread)
# ... (9개의 I/O 스레드)

# I/O 스레드 상태
# - 총 9개의 I/O 스레드
# - 모두 "waiting" 상태 (요청 대기 중)
# ✅ 정상 (요청이 없으면 대기)

Pending normal aio reads: [0, 0, 0, 0] , aio writes: [0, 0, 0, 0] ,
 ibuf aio reads:
Pending flushes (fsync) log: 0; buffer pool: 0
# - Pending reads/writes: 0 (대기 중인 I/O 없음)
# - Pending fsync: 0 (디스크 동기화 대기 없음)
# ✅ I/O 병목 없음

2121 OS file reads, 50128 OS file writes, 31681 OS fsyncs
# 누적 통계 (MySQL 시작 후)
# - 파일 읽기: 2,121번
# - 파일 쓰기: 50,128번
# - fsync: 31,681번
#
# 💡 쓰기가 많은 이유:
# - k6 테스트로 대량 INSERT/UPDATE 발생
# - 정상적인 부하 테스트 결과

0.00 reads/s, 0 avg bytes/read, 0.00 writes/s, 0.00 fsyncs/s
# 현재 I/O 속도: 0 (k6 테스트 중지됨)

-------------------------------------
INSERT BUFFER AND ADAPTIVE HASH INDEX
-------------------------------------
Ibuf: size 1, free list len 0, seg size 2, 6 merges
merged operations:
 insert 17, delete mark 0, delete 0
discarded operations:
 insert 0, delete mark 0, delete 0

# Insert Buffer (Secondary Index 최적화)
# - size 1: 버퍼 크기 1페이지
# - 6 merges: 6번 병합 발생
# - 17개 insert 병합됨
# ✅ 정상 작동

Hash table size 34679, node heap has 4 buffer(s)
# ... (8개의 해시 테이블)

# Adaptive Hash Index (자주 조회되는 데이터 캐싱)
# - 총 8개의 해시 테이블
# - 총 11개의 버퍼 사용
# ✅ 정상

0.00 hash searches/s, 0.00 non-hash searches/s
# 현재 검색 속도: 0 (k6 테스트 중지)

---
LOG
---
Log sequence number          169066688
Log buffer assigned up to    169066688
Log buffer completed up to   169066688
Log written up to            169066688
Log flushed up to            169066688
Added dirty pages up to      169066688
Pages flushed up to          169066688
Last checkpoint at           169066688

# Redo Log 상태
# - Log sequence number: 169,066,688 (현재 로그 위치)
# - 모든 로그가 동일한 위치: 완전히 동기화됨
# - Last checkpoint: 마지막 체크포인트도 동일
#
# 💡 의미:
# - 모든 트랜잭션이 디스크에 완전히 기록됨
# - 복구 필요 없음
# ✅ 완벽한 일관성

Log minimum file id is       48
Log maximum file id is       51
# Redo Log 파일 범위: 48~51 (4개 파일)
# ✅ 정상 순환 중

47510 log i/o's done, 0.00 log i/o's/second
# - 누적 로그 I/O: 47,510번
# - 현재 속도: 0 (k6 테스트 중지)

----------------------
BUFFER POOL AND MEMORY
----------------------
Total large memory allocated 0
Dictionary memory allocated 762215
# - 대용량 메모리: 0바이트
# - Dictionary 메모리: 762KB
# 💡 Dictionary: 테이블/인덱스 메타데이터

Buffer pool size   8192
# Buffer Pool 크기: 8,192 페이지
# = 8,192 × 16KB = 128MB
#
# ⚠️ 너무 작음!
# 권장: RAM의 70~80% (예: 4GB RAM → 3GB Buffer Pool)

Free buffers       6465
Database pages     1716
Old database pages 650
# - Free: 6,465 페이지 (78.9% 비어있음)
# - 사용 중: 1,716 페이지 (데이터)
# - Old pages: 650 페이지 (오래된 데이터)
#
# 💡 의미:
# - Buffer Pool 여유 충분
# - 현재 데이터가 적음 (테스트 환경)

Modified db pages  0
# Dirty Pages: 0
# 💡 모든 변경사항이 디스크에 기록됨
# ✅ 완전히 동기화됨

Pending reads      0
Pending writes: LRU 0, flush list 0, single page 0
# - Pending reads: 0 (대기 중인 읽기 없음)
# - Pending writes: 0 (대기 중인 쓰기 없음)
# ✅ I/O 병목 없음

Pages made young 3, not young 0
0.00 youngs/s, 0.00 non-youngs/s
# Buffer Pool LRU 알고리즘 통계
# - young: 3페이지가 최근 사용으로 이동
# - not young: 0페이지 (이동 안 함)
# ✅ 정상 작동

Pages read 1450, created 266, written 2075
# 누적 통계:
# - 읽기: 1,450 페이지
# - 생성: 266 페이지 (새로운 페이지)
# - 쓰기: 2,075 페이지
#
# 💡 쓰기가 많은 이유:
# - k6 테스트로 대량 INSERT/UPDATE

0.00 reads/s, 0.00 creates/s, 0.00 writes/s
# 현재 속도: 0 (k6 테스트 중지)

No buffer pool page gets since the last printout
Pages read ahead 0.00/s, evicted without access 0.00/s, Random read ahead 0.00/s
# - Read-ahead: 0 (미리 읽기 없음)
# - Evicted: 0 (캐시에서 제거 없음)
# ✅ 캐시 히트율 좋음

LRU len: 1716, unzip_LRU len: 0
I/O sum[0]:cur[0], unzip sum[0]:cur[0]
# - LRU 리스트 길이: 1,716 페이지
# - 압축 해제 리스트: 0 (압축된 페이지 없음)

--------------
ROW OPERATIONS  # 🎯 가장 중요한 섹션!
--------------
0 queries inside InnoDB, 0 queries in queue
# - 현재 실행 중인 쿼리: 0개
# - 대기 중인 쿼리: 0개
# ✅ k6 테스트 중지됨

0 read views open inside InnoDB
# Read View: 0개
# 💡 Read View = MVCC를 위한 스냅샷
# 트랜잭션이 없으면 Read View도 없음

Process ID=1, Main thread ID=281473005186816 , state=sleeping
# MySQL 메인 스레드: sleeping (유휴 상태)
# ✅ 정상

Number of rows inserted 6959, updated 13265, deleted 1482, read 103585
# 📊 누적 통계 (MySQL 시작 후)
# - INSERT: 6,959건
# - UPDATE: 13,265건 ⚠️ (가장 많음!)
# - DELETE: 1,482건
# - READ: 103,585건
#
# 💡 UPDATE가 많은 이유:
# - k6 테스트: 조회수, 좋아요, 댓글 업데이트
# - 각 업데이트마다 전체 컬럼 UPDATE (JPA Dirty Checking)
#
# ⚠️ 문제:
# 우리가 JPQL Atomic Update를 적용했다면
# UPDATE 수가 훨씬 적어야 함!
# → 애플리케이션 재시작 필요!

0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s
# 현재 속도: 0 (k6 테스트 중지)

Number of system rows inserted 8, updated 331, deleted 8, read 7279
# 시스템 테이블 통계 (메타데이터)
# - UPDATE 331건: 통계 정보 업데이트
# ✅ 정상

0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s
# 현재 속도: 0

----------------------------
END OF INNODB MONITOR OUTPUT
============================
```

---

## 🚨 핵심 문제점

### 1. 데드락 발생 확인
```sql
LATEST DETECTED DEADLOCK
2025-11-20 00:58:05

원인: Lock Upgrade Deadlock
- 트랜잭션 1: S-Lock 보유 → X-Lock 대기
- 트랜잭션 2: S-Lock 보유 → X-Lock 대기
→ 서로 막힘!
```

### 2. JPA Dirty Checking 사용 중
```sql
update community_posts
set user_id=9999,
    category='GENERAL',
    comment_count=2791,
    content='...',
    is_deleted=0,
    is_modified=0,
    like_count=34,
    modified_at=null,
    title='...',
    view_count=10383
where post_id=1
```

**이건 우리가 구현한 JPQL Atomic이 아님!**

### 3. 애플리케이션 재시작 필요
```
예상 쿼리 (JPQL Atomic):
UPDATE community_posts SET view_count = view_count + 1 WHERE post_id = 1

실제 쿼리 (JPA Dirty Checking):
UPDATE community_posts SET user_id=..., category=..., comment_count=... WHERE post_id = 1

결론: 예전 코드가 실행 중!
```

---

## 📊 성능 통계 분석

### ROW OPERATIONS (누적)
```
INSERT:  6,959건
UPDATE: 13,265건  ⚠️ 가장 많음!
DELETE:  1,482건
READ:  103,585건

UPDATE가 많은 이유:
- k6 테스트로 조회수, 좋아요, 댓글 업데이트
- JPA Dirty Checking으로 전체 컬럼 업데이트
```

### Buffer Pool
```
크기: 128MB (8,192 페이지 × 16KB)
사용: 1,716 페이지 (21%)
여유: 6,465 페이지 (79%)

⚠️ 문제:
- 128MB는 너무 작음!
- 권장: RAM의 70~80%
- 예: 4GB RAM → 3GB Buffer Pool
```

### File I/O
```
누적 통계:
- 파일 읽기: 2,121번
- 파일 쓰기: 50,128번  ⚠️ 쓰기 많음!
- fsync: 31,681번

쓰기가 많은 이유:
- k6 부하 테스트
- 대량 INSERT/UPDATE
```

---

## ✅ 해결 방법

### 1. 애플리케이션 재시작 (필수!)
```bash
# 기존 프로세스 종료
kill 77590

# 재시작
./gradlew clean bootRun &
```

### 2. JPQL Atomic Update 확인
```sql
-- 기대하는 쿼리
UPDATE community_posts SET view_count = view_count + 1 WHERE post_id = ?
UPDATE community_posts SET like_count = like_count + 1 WHERE post_id = ?
UPDATE community_posts SET comment_count = comment_count + 1 WHERE post_id = ?
```

### 3. k6 테스트 재실행
```bash
k6 run k6-tests/mix_test.js
```

### 4. 데드락 로그 확인
```bash
docker exec homesweet-db mysql -u root -prootpassword homesweet \
  -e "SHOW ENGINE INNODB STATUS\G" | grep -A 100 "LATEST DETECTED DEADLOCK"

# 데드락 발생 안 하는지 확인!
```

---

## 🎓 학습 포인트

### 데드락 패턴 이해
```
Lock Upgrade Deadlock:
1. 트랜잭션 A: SELECT ... FOR UPDATE (S-Lock)
2. 트랜잭션 B: SELECT ... FOR UPDATE (S-Lock)
3. 트랜잭션 A: UPDATE (X-Lock 요청) → 대기 (B가 S-Lock 보유)
4. 트랜잭션 B: UPDATE (X-Lock 요청) → 대기 (A가 S-Lock 보유)
→ 데드락!

해결:
- JPQL Atomic Update 사용
- SELECT 없이 바로 UPDATE
- S-Lock → X-Lock 업그레이드 없음
```

### InnoDB 락 동작
```
REPEATABLE-READ 격리 수준:
1. SELECT ... FOR UPDATE
   → 먼저 S-Lock (일관성 있는 읽기)
   → 나중에 X-Lock 업그레이드 (쓰기)

2. UPDATE (직접)
   → 바로 X-Lock
   → 락 업그레이드 없음 (빠름!)
```

### Buffer Pool 중요성
```
현재: 128MB
권장: 3GB (RAM 4GB 기준)

효과:
- 디스크 I/O 감소
- 쿼리 속도 향상
- 2~3배 성능 개선
```

---

## 📝 체크리스트

- [ ] 애플리케이션 재시작
- [ ] JPQL Atomic Update 쿼리 확인
- [ ] k6 테스트 재실행
- [ ] 데드락 로그 확인 (없어야 함)
- [ ] Buffer Pool 크기 증가 검토
- [ ] 복합 인덱스 추가 검토

---

**다음 단계: 애플리케이션 재시작!** 🚀
