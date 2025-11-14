/**
 * 테스트 데이터 생성 스크립트
 *
 * 실행 방법:
 * k6 run k6-tests/setup-test-data.js
 *
 * 주의: 이 스크립트는 인증이 필요한 API를 호출합니다.
 * 실제 환경에서는 OAuth2 토큰을 받아서 사용해야 합니다.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  console.log('📝 테스트 데이터 생성 시작...');

  // 게시글 목록 확인
  const listResponse = http.get(`${BASE_URL}/api/v1/community/posts?page=0&size=10`);

  console.log(`현재 게시글 수: ${listResponse.json('totalElements') || 0}`);

  const hasData = check(listResponse, {
    'list status is 200': (r) => r.status === 200,
  });

  if (hasData) {
    const posts = listResponse.json('content');
    if (posts && posts.length > 0) {
      console.log(`✅ 테스트 가능한 게시글 ${posts.length}개 발견`);
      console.log(`게시글 ID: ${posts.map(p => p.postId).join(', ')}`);
      return;
    }
  }

  console.log('⚠️  게시글이 없습니다.');
  console.log('');
  console.log('📌 테스트 데이터 생성 방법:');
  console.log('');
  console.log('1. Swagger UI 사용:');
  console.log('   http://localhost:8080/swagger-ui/index.html');
  console.log('   - OAuth2 로그인 후 게시글 작성');
  console.log('');
  console.log('2. MySQL 직접 삽입:');
  console.log('   docker exec -it homesweet-db mysql -u user -ppassword homesweet');
  console.log('   INSERT INTO community_posts ...');
  console.log('');
  console.log('3. 통합 테스트 실행:');
  console.log('   ./gradlew test --tests "*CommunityPostServiceIntegratTest*"');
  console.log('');
}

export function teardown() {
  console.log('');
  console.log('✨ 데이터 확인 완료');
}
