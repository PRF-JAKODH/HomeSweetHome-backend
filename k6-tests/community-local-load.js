/**
 * 로컬 PC 환경용 커뮤니티 부하 테스트
 *
 * 로컬 환경 특성:
 * - DB: Docker MySQL (localhost)
 * - 애플리케이션: Spring Boot (localhost:8080)
 * - 하드웨어: 개발용 PC
 *
 * 실행 방법:
 * k6 run k6-tests/community-local-load.js
 *
 * 목적:
 * - 로컬에서 동시성 제어 로직 검증
 * - 병목 지점 파악
 * - 성능 프로파일링 데이터 수집
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const viewDuration = new Trend('view_duration');
const listDuration = new Trend('list_duration');

// 로컬 PC에 맞는 단계적 부하
export const options = {
  stages: [
    // 워밍업: 시스템이 준비되도록
    { duration: '15s', target: 10 },

    // 점진적 증가: 로컬 DB 부하 확인
    { duration: '30s', target: 30 },
    { duration: '30s', target: 50 },

    // 피크: 로컬에서 감당 가능한 최대치
    { duration: '1m', target: 100 },

    // 쿨다운
    { duration: '20s', target: 0 },
  ],

  // 로컬 환경 임계값 (프로덕션보다 관대)
  thresholds: {
    'http_req_duration': ['p(90)<3000', 'p(95)<5000'],  // 로컬 DB 고려
    'http_req_failed': ['rate<0.15'],                    // 15% 미만
    'errors': ['rate<0.2'],                              // 20% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('⚡ 로컬 환경 부하 테스트');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`📍 Target: ${BASE_URL}`);
  console.log('📊 Max VUs: 100 (로컬 PC 최적화)');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  // 서버 헬스체크
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  if (healthCheck.status !== 200) {
    console.error('❌ 서버가 응답하지 않습니다. 서버를 먼저 실행해주세요.');
    throw new Error('Server not ready');
  }

  console.log('✅ 서버 준비 완료\n');
  return { startTime: Date.now() };
}

export default function () {
  // ══════════════════════════════════════════════════════
  // 1. 게시글 목록 조회 (가장 빈번한 API)
  // ══════════════════════════════════════════════════════
  const page = Math.floor(Math.random() * 5); // 0~4 페이지
  const listResponse = http.get(
    `${BASE_URL}/api/v1/community/posts?page=${page}&size=20`,
    { tags: { name: 'GetPostList' } }
  );

  const listCheck = check(listResponse, {
    '목록 조회 성공': (r) => r.status === 200,
    '응답 시간 < 3초': (r) => r.timings.duration < 3000,
  });

  if (!listCheck) {
    errorRate.add(1);
  } else {
    listDuration.add(listResponse.timings.duration);
  }

  sleep(0.5);

  // ══════════════════════════════════════════════════════
  // 2. 게시글 상세 조회
  // ══════════════════════════════════════════════════════
  let postId;
  try {
    const posts = listResponse.json('content');
    if (posts && posts.length > 0) {
      postId = posts[Math.floor(Math.random() * posts.length)].postId;
    } else {
      // 데이터가 없으면 스킵
      return;
    }
  } catch (e) {
    errorRate.add(1);
    return;
  }

  const detailResponse = http.get(
    `${BASE_URL}/api/v1/community/posts/${postId}`,
    { tags: { name: 'GetPostDetail' } }
  );

  check(detailResponse, {
    '상세 조회 성공': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);

  // ══════════════════════════════════════════════════════
  // 3. 조회수 증가 (동시성 테스트의 핵심!)
  // ══════════════════════════════════════════════════════
  const viewResponse = http.post(
    `${BASE_URL}/api/v1/community/posts/${postId}/views`,
    null,
    { tags: { name: 'IncreaseView' } }
  );

  const viewCheck = check(viewResponse, {
    '조회수 증가 성공': (r) => r.status === 200,
  });

  if (viewCheck) {
    viewDuration.add(viewResponse.timings.duration);
  } else {
    errorRate.add(1);
  }

  sleep(0.5);

  // ══════════════════════════════════════════════════════
  // 4. 댓글 조회
  // ══════════════════════════════════════════════════════
  const commentsResponse = http.get(
    `${BASE_URL}/api/v1/community/posts/${postId}/comments`,
    { tags: { name: 'GetComments' } }
  );

  check(commentsResponse, {
    '댓글 조회 성공': (r) => r.status === 200,
  }) || errorRate.add(1);

  // 랜덤 대기 (실제 사용자 행동 시뮬레이션)
  sleep(Math.random() * 2);
}

export function handleSummary(data) {
  const avgDuration = data.metrics.http_req_duration.values.avg;
  const p95Duration = data.metrics.http_req_duration.values['p(95)'];
  const errorRate = data.metrics.http_req_failed ?
    (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 0;

  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('📊 부하 테스트 결과 요약');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`✅ 총 요청 수: ${data.metrics.http_reqs.values.count}`);
  console.log(`⏱️  평균 응답 시간: ${avgDuration.toFixed(2)}ms`);
  console.log(`📈 95th 백분위: ${p95Duration.toFixed(2)}ms`);
  console.log(`❌ 에러율: ${errorRate}%`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  // 성능 평가
  if (p95Duration < 1000) {
    console.log('🎉 훌륭합니다! 로컬 환경에서 우수한 성능입니다.');
  } else if (p95Duration < 3000) {
    console.log('👍 양호합니다. 로컬 환경에서 적절한 성능입니다.');
  } else {
    console.log('⚠️  응답 시간이 느립니다. 병목 지점을 확인하세요.');
  }

  return {
    'summary.txt': JSON.stringify(data, null, 2),
  };
}

export function teardown(data) {
  const duration = ((Date.now() - data.startTime) / 1000).toFixed(1);
  console.log(`\n⏱️  총 테스트 시간: ${duration}초\n`);
}
