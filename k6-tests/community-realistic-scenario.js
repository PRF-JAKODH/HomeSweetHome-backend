/**
 * 로컬 환경용 커뮤니티 실제 사용자 시나리오 테스트
 *
 * 실행 방법:
 * 1. 조회만 (인증 불필요):
 *    k6 run k6-tests/community-realistic-scenario.js
 *
 * 2. 전체 기능 테스트 (인증 필요):
 *    export TEST_TOKEN="Bearer your-jwt-token"
 *    k6 run k6-tests/community-realistic-scenario.js
 *
 * 토큰 발급 방법:
 * - 브라우저에서 로그인 후 개발자 도구 > Application > Local Storage에서 토큰 복사
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const apiDuration = new Trend('api_duration');
const likeCounter = new Counter('likes_count');
const commentCounter = new Counter('comments_count');

// 로컬 PC에 맞는 낮은 부하 설정
export const options = {
  stages: [
    { duration: '20s', target: 5 },    // 20초 동안 5명까지
    { duration: '40s', target: 10 },   // 40초 동안 10명 유지
    { duration: '20s', target: 0 },    // 20초 동안 종료
  ],

  thresholds: {
    'http_req_duration': ['p(95)<2000'],  // 로컬 DB 고려
    'http_req_failed': ['rate<0.1'],
    'errors': ['rate<0.15'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.TEST_TOKEN || '';
const HAS_AUTH = AUTH_TOKEN.length > 0;

export function setup() {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🏠 커뮤니티 실제 사용자 시나리오 테스트');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`📍 Base URL: ${BASE_URL}`);
  console.log(`🔐 인증: ${HAS_AUTH ? '✅ 활성화 (전체 기능)' : '❌ 비활성화 (조회만)'}`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  if (!HAS_AUTH) {
    console.log('💡 Tip: 전체 기능 테스트를 원하면:');
    console.log('   export TEST_TOKEN="Bearer your-token"');
    console.log('   k6 run k6-tests/community-realistic-scenario.js\n');
  }

  return { startTime: Date.now() };
}

export default function () {
  const headers = HAS_AUTH ? {
    'Authorization': AUTH_TOKEN,
    'Content-Type': 'application/json',
  } : {};

  // ════════════════════════════════════════════════════════════
  // 시나리오 1: 메인 페이지 진입 - 게시글 목록 조회
  // ════════════════════════════════════════════════════════════
  const listResponse = http.get(
    `${BASE_URL}/api/v1/community/posts?page=0&size=10&sort=createdAt&direction=DESC`,
    { tags: { name: 'ListPosts' } }
  );

  const listCheck = check(listResponse, {
    '목록 조회 성공': (r) => r.status === 200,
    '목록에 데이터 있음': (r) => {
      try {
        const data = r.json();
        return data.content && data.content.length > 0;
      } catch {
        return false;
      }
    },
  });

  if (!listCheck) {
    errorRate.add(1);
    console.warn(`⚠️  목록 조회 실패 (status: ${listResponse.status})`);
    return; // 목록이 없으면 더 이상 진행 불가
  }

  apiDuration.add(listResponse.timings.duration);
  sleep(1); // 사용자가 목록 훑어보는 시간

  // ════════════════════════════════════════════════════════════
  // 시나리오 2: 관심 있는 게시글 클릭 - 상세 조회
  // ════════════════════════════════════════════════════════════
  let postId;
  try {
    const posts = listResponse.json('content');
    // 랜덤하게 게시글 선택 (실제 사용자처럼)
    postId = posts[Math.floor(Math.random() * Math.min(posts.length, 5))].postId;
  } catch (e) {
    console.error('게시글 ID 추출 실패:', e);
    errorRate.add(1);
    return;
  }

  const detailResponse = http.get(
    `${BASE_URL}/api/v1/community/posts/${postId}`,
    { tags: { name: 'GetPostDetail' } }
  );

  const detailCheck = check(detailResponse, {
    '상세 조회 성공': (r) => r.status === 200,
    '게시글 제목 있음': (r) => r.json('title') !== undefined,
    '게시글 내용 있음': (r) => r.json('content') !== undefined,
  });

  if (!detailCheck) {
    errorRate.add(1);
  } else {
    apiDuration.add(detailResponse.timings.duration);
  }

  sleep(2); // 게시글 읽는 시간

  // ════════════════════════════════════════════════════════════
  // 시나리오 3: 조회수 증가 (인증 불필요)
  // ════════════════════════════════════════════════════════════
  const viewResponse = http.post(
    `${BASE_URL}/api/v1/community/posts/${postId}/views`,
    null,
    { tags: { name: 'IncreaseView' } }
  );

  check(viewResponse, {
    '조회수 증가 성공': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);

  // ════════════════════════════════════════════════════════════
  // 시나리오 4: 댓글 조회
  // ════════════════════════════════════════════════════════════
  const commentsResponse = http.get(
    `${BASE_URL}/api/v1/community/posts/${postId}/comments`,
    { tags: { name: 'GetComments' } }
  );

  const hasComments = check(commentsResponse, {
    '댓글 조회 성공': (r) => r.status === 200,
  });

  if (!hasComments) {
    errorRate.add(1);
  }

  sleep(1.5); // 댓글 읽는 시간

  // ════════════════════════════════════════════════════════════
  // 시나리오 5: 좋아요 (인증 필요) - 30% 확률
  // ════════════════════════════════════════════════════════════
  if (HAS_AUTH && Math.random() < 0.3) {
    const likeResponse = http.post(
      `${BASE_URL}/api/v1/community/posts/${postId}/likes`,
      null,
      {
        headers: headers,
        tags: { name: 'ToggleLike' },
      }
    );

    const liked = check(likeResponse, {
      '좋아요 성공': (r) => r.status === 200,
    });

    if (liked) {
      likeCounter.add(1);
    } else {
      errorRate.add(1);
    }

    sleep(0.5);
  }

  // ════════════════════════════════════════════════════════════
  // 시나리오 6: 댓글 작성 (인증 필요) - 20% 확률
  // ════════════════════════════════════════════════════════════
  if (HAS_AUTH && Math.random() < 0.2) {
    const comments = [
      '좋은 정보 감사합니다!',
      '저도 이거 궁금했는데 도움됐어요',
      '사진 너무 예쁘네요',
      '링크 공유 부탁드려요',
      '대박입니다 ㅎㅎ',
    ];

    const commentPayload = JSON.stringify({
      content: comments[Math.floor(Math.random() * comments.length)],
    });

    const commentResponse = http.post(
      `${BASE_URL}/api/v1/community/posts/${postId}/comments`,
      commentPayload,
      {
        headers: headers,
        tags: { name: 'CreateComment' },
      }
    );

    const commented = check(commentResponse, {
      '댓글 작성 성공': (r) => r.status === 201,
    });

    if (commented) {
      commentCounter.add(1);
    } else {
      errorRate.add(1);
      console.warn(`댓글 작성 실패 (status: ${commentResponse.status}): ${commentResponse.body.substring(0, 100)}`);
    }

    sleep(1);
  }

  // ════════════════════════════════════════════════════════════
  // 시나리오 7: 뒤로가기 또는 다른 게시글 보기 (50% 확률)
  // ════════════════════════════════════════════════════════════
  if (Math.random() < 0.5) {
    // 다른 페이지 조회
    const page = Math.floor(Math.random() * 3); // 0~2 페이지
    http.get(
      `${BASE_URL}/api/v1/community/posts?page=${page}&size=10`,
      { tags: { name: 'BrowseMore' } }
    );

    sleep(2);
  }

  // 다음 액션까지 랜덤 대기
  sleep(Math.random() * 2);
}

export function teardown(data) {
  const duration = ((Date.now() - data.startTime) / 1000).toFixed(1);
  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('✅ 테스트 완료');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`⏱️  총 소요 시간: ${duration}초`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}
