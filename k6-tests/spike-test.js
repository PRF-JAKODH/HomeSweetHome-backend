/**
 * 스파이크 테스트 - 급격한 트래픽 증가 대응
 *
 * 실행 방법:
 * k6 run k6-tests/spike-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 },    // 정상 트래픽
    { duration: '10s', target: 5000 },   // 갑자기 500명! (스파이크)
    { duration: '30s', target: 100 },   // 30초 유지
    { duration: '10s', target: 10 },    // 다시 정상으로
    { duration: '20s', target: 10 },    // 회복 시간
  ],

  thresholds: {
    'http_req_duration': ['p(95)<1000'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 게시글 목록 조회 (N+1 문제 확인)
  const response = http.get(`${BASE_URL}/api/v1/community/posts?page=0&size=20`);

  check(response, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(Math.random() * 2);
}
