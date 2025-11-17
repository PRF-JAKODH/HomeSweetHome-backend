/**
 * 빠른 커뮤니티 기능 검증 테스트 (1분 완료)
 *
 * 용도:
 * - 코드 수정 후 빠른 동작 확인
 * - CI/CD 파이프라인 통합용
 * - 로컬 개발 중 sanity check
 *
 * 실행:
 * k6 run k6-tests/community-quick-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,              // 동시 사용자 5명
  duration: '1m',       // 1분간 실행
  thresholds: {
    'http_req_failed': ['rate<0.1'],  // 90% 이상 성공
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  console.log('🚀 빠른 기능 검증 시작 (1분 소요)...\n');
}

export default function () {
  // 1. 목록 조회
  const list = http.get(`${BASE_URL}/api/v1/community/posts?page=0&size=10`);

  if (!check(list, { '목록 조회': (r) => r.status === 200 })) {
    console.error('❌ 목록 조회 실패');
    return;
  }

  sleep(1);

  // 2. 상세 조회
  try {
    const posts = list.json('content');
    if (posts && posts.length > 0) {
      const postId = posts[0].postId;

      const detail = http.get(`${BASE_URL}/api/v1/community/posts/${postId}`);
      check(detail, { '상세 조회': (r) => r.status === 200 });

      sleep(1);

      // 3. 조회수 증가
      const view = http.post(`${BASE_URL}/api/v1/community/posts/${postId}/views`);
      check(view, { '조회수 증가': (r) => r.status === 200 });

      sleep(1);

      // 4. 댓글 조회
      const comments = http.get(`${BASE_URL}/api/v1/community/posts/${postId}/comments`);
      check(comments, { '댓글 조회': (r) => r.status === 200 });
    }
  } catch (e) {
    console.error('테스트 실패:', e);
  }

  sleep(2);
}

export function teardown() {
  console.log('\n✅ 빠른 검증 완료!\n');
}
