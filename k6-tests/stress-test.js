/**
 * 스트레스 테스트 - 시스템 한계 찾기
 *
 * 실행 방법:
 * k6 run k6-tests/stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 9000 },    // 1초 동안 5000명
  ],

  thresholds: {
    'http_req_duration': ['p(99)<20000'], // 99%가 2초 이내
    'http_req_failed': ['rate<0.05'],     // 에러율 5% 미만
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 조회수 증가 API에 집중 (동시성 제어 테스트)
  const postId = Math.floor(Math.random() * 10) + 1; // 1~10번 게시글 랜덤

  const response = http.post(`${BASE_URL}/api/v1/community/posts/${postId}/views`);

  check(response, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.5); // 짧은 대기
}
