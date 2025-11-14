/**
 * 커뮤니티 API 부하 테스트
 *
 * 실행 방법:
 * k6 run k6-tests/community-load-test.js
 *
 * Prometheus 통합:
 * k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭 정의
const errorRate = new Rate('errors');
const postViewTrend = new Trend('post_view_duration');
const postListTrend = new Trend('post_list_duration');
const postCreateCounter = new Counter('post_created');

// 테스트 설정
export const options = {
  // 부하 테스트 시나리오
  stages: [
    { duration: '30s', target: 10 },   // 30초 동안 10명까지 증가
    { duration: '1m', target: 50 },    // 1분 동안 50명까지 증가
    { duration: '2m', target: 100 },   // 2분 동안 100명 유지
    { duration: '30s', target: 0 },    // 30초 동안 0명으로 감소
  ],

  // 성능 임계값 설정
  thresholds: {
    'http_req_duration': ['p(95)<500'],  // 95%의 요청이 500ms 이내
    'http_req_failed': ['rate<0.01'],     // 에러율 1% 미만
    'errors': ['rate<0.1'],               // 커스텀 에러율 10% 미만
  },

  // 태그 (Prometheus에서 라벨로 사용)
  tags: {
    testType: 'load',
    api: 'community',
  },
};

const BASE_URL = 'http://localhost:8080';

// 테스트용 더미 토큰 (실제로는 로그인 후 받은 토큰 사용)
const AUTH_TOKEN = 'Bearer your-test-token-here';

export function setup() {
  console.log('🚀 부하 테스트 시작');
  console.log(`Base URL: ${BASE_URL}`);
  return { startTime: new Date() };
}

export default function () {
  // 1. 게시글 목록 조회 (가장 빈번한 요청)
  const listResponse = http.get(`${BASE_URL}/api/v1/community/posts?page=0&size=10`, {
    tags: { name: 'GetPostList' },
  });

  const listCheckResult = check(listResponse, {
    'list status is 200': (r) => r.status === 200,
    'list has data': (r) => r.json('content') !== undefined,
  });

  if (!listCheckResult) {
    errorRate.add(1);
  }

  postListTrend.add(listResponse.timings.duration);

  sleep(1); // 사용자가 목록을 읽는 시간

  // 2. 목록에서 게시글 ID 가져오기
  let postId;
  try {
    const posts = listResponse.json('content');
    if (posts && posts.length > 0) {
      // 랜덤하게 게시글 선택
      postId = posts[Math.floor(Math.random() * posts.length)].postId;
    } else {
      // 게시글이 없으면 스킵
      console.warn('No posts available, skipping detail views');
      return;
    }
  } catch (e) {
    console.error('Failed to parse post list:', e);
    errorRate.add(1);
    return;
  }

  // 3. 특정 게시글 조회
  const viewResponse = http.get(`${BASE_URL}/api/v1/community/posts/${postId}`, {
    tags: { name: 'GetPost' },
  });

  const viewCheckResult = check(viewResponse, {
    'view status is 200': (r) => r.status === 200,
    'view has postId': (r) => r.json('postId') !== undefined,
  });

  if (!viewCheckResult) {
    errorRate.add(1);
  } else {
    postViewTrend.add(viewResponse.timings.duration);
  }

  sleep(2); // 사용자가 게시글을 읽는 시간

  // 4. 조회수 증가 (동시성 테스트 포인트!)
  const viewCountResponse = http.post(
    `${BASE_URL}/api/v1/community/posts/${postId}/views`,
    null,
    {
      tags: { name: 'IncreaseViewCount' },
    }
  );

  const viewCountCheckResult = check(viewCountResponse, {
    'view count status is 200': (r) => r.status === 200,
  });

  if (!viewCountCheckResult) {
    errorRate.add(1);
  }

  sleep(1);

  // 5. 댓글 조회
  const commentsResponse = http.get(
    `${BASE_URL}/api/v1/community/posts/${postId}/comments`,
    {
      tags: { name: 'GetComments' },
    }
  );

  const commentsCheckResult = check(commentsResponse, {
    'comments status is 200': (r) => r.status === 200,
  });

  if (!commentsCheckResult) {
    errorRate.add(1);
  }

  sleep(Math.random() * 3); // 랜덤한 대기 시간
}

export function teardown(data) {
  const endTime = new Date();
  const duration = (endTime - data.startTime) / 1000;
  console.log(`✅ 부하 테스트 완료 (소요 시간: ${duration}초)`);
}
