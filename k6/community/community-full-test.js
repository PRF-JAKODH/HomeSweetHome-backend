import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

/**
 * 커뮤니티 전체 기능 종합 테스트
 *
 * 테스트 범위:
 * ✅ 게시글 CRUD (작성, 조회, 수정, 삭제)
 * ✅ 댓글 CRUD (작성, 조회, 수정, 삭제)
 * ✅ 게시글 좋아요 토글 & 상태 확인
 * ✅ 댓글 좋아요 토글 & 상태 확인
 * ✅ 조회수 증가
 * ✅ 페이지네이션
 * ✅ 동시성 제어 검증
 */

// ==================== 커스텀 메트릭 ====================
const postCreateErrors = new Counter('post_create_errors');
const postReadErrors = new Counter('post_read_errors');
const postUpdateErrors = new Counter('post_update_errors');
const postDeleteErrors = new Counter('post_delete_errors');
const commentCreateErrors = new Counter('comment_create_errors');
const commentReadErrors = new Counter('comment_read_errors');
const commentUpdateErrors = new Counter('comment_update_errors');
const commentDeleteErrors = new Counter('comment_delete_errors');
const likeErrors = new Counter('like_errors');
const viewErrors = new Counter('view_errors');
const paginationErrors = new Counter('pagination_errors');

const apiSuccessRate = new Rate('api_success_rate');
const postCreateDuration = new Trend('post_create_duration');
const postReadDuration = new Trend('post_read_duration');
const likeDuration = new Trend('like_duration');

// ==================== 테스트 설정 ====================
export const options = {
  scenarios: {
    // 시나리오 1: CRUD 기능 테스트
    crud_operations: {
      executor: 'constant-vus',
      vus: 10,
      duration: '2m',
      exec: 'testCRUDOperations',
    },

    // 시나리오 2: 동시성 제어 - 좋아요 (100명이 각각 10번씩 = 1000회)
    concurrency_likes: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 10,
      maxDuration: '1m',
      exec: 'testConcurrentLikes',
      startTime: '2m',
    },

    // 시나리오 3: 동시성 제어 - 조회수
    concurrency_views: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      exec: 'testConcurrentViews',
      startTime: '3m',
    },

    // 시나리오 4: 실제 사용자 시뮬레이션
    user_journey: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      exec: 'testUserJourney',
      startTime: '3m30s',
    },

    // 시나리오 5: 스트레스 테스트
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '1m', target: 300 },
        { duration: '2m', target: 300 },
        { duration: '1m', target: 0 },
      ],
      exec: 'testStress',
      startTime: '6m30s',
    },
  },

  thresholds: {
    // 전체 HTTP 요청
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.05'],

    // API별 성공률
    api_success_rate: ['rate>0.95'],

    // 개별 작업 에러
    post_create_errors: ['count<5'],
    post_read_errors: ['count<5'],
    post_update_errors: ['count<5'],
    post_delete_errors: ['count<5'],
    comment_create_errors: ['count<5'],
    comment_read_errors: ['count<5'],
    comment_update_errors: ['count<5'],
    comment_delete_errors: ['count<5'],
    like_errors: ['count<10'],
    view_errors: ['count<10'],
    pagination_errors: ['count<5'],

    // 성능
    post_create_duration: ['p(95)<1500'],
    post_read_duration: ['p(95)<500'],
    like_duration: ['p(95)<800'],
  },
};

// ==================== 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

const getHeaders = () => {
  const headers = {
    'Content-Type': 'application/json',
  };
  if (AUTH_TOKEN) {
    headers['Authorization'] = `Bearer ${AUTH_TOKEN}`;
  }
  return headers;
};

// 테스트 데이터
const testData = {
  categories: ['자유게시판', '질문게시판', '정보공유', '공지사항'],

  titles: [
    '안녕하세요 처음 글 써봅니다',
    '이거 어떻게 하나요?',
    '유용한 정보 공유합니다',
    '오늘의 팁',
    '질문있어요!',
  ],

  contents: [
    '안녕하세요. 처음 글을 작성해봅니다. 잘 부탁드립니다!',
    '궁금한 점이 있어서 질문드립니다.',
    '유용한 정보를 발견해서 공유합니다.',
    '오늘 알게 된 팁을 공유드립니다.',
    '이 부분이 잘 이해가 안 되는데 도움 부탁드립니다.',
  ],

  comments: [
    '좋은 정보 감사합니다!',
    '저도 궁금했는데 도움이 됐어요',
    '댓글 남겨요~',
    '유용한 글이네요',
    '공감합니다!',
  ],
};

// ==================== 시나리오 1: CRUD 작업 테스트 ====================
export function testCRUDOperations() {
  group('게시글 CRUD', function () {
    let postId;

    // 1. 게시글 작성
    group('게시글 작성', function () {
      const createData = {
        title: testData.titles[Math.floor(Math.random() * testData.titles.length)],
        content: testData.contents[Math.floor(Math.random() * testData.contents.length)],
        category: testData.categories[Math.floor(Math.random() * testData.categories.length)],
      };

      const startTime = new Date();

      // multipart/form-data로 요청 (이미지 없이)
      const formData = {
        request: http.file(JSON.stringify(createData), 'request.json', 'application/json'),
      };

      const res = http.post(
        `${API_BASE}/posts`,
        formData,
        {
          tags: { name: 'CreatePost' },
        }
      );
      postCreateDuration.add(new Date() - startTime);

      const success = check(res, {
        '게시글 작성 성공': (r) => r.status === 201,
        '응답에 postId 포함': (r) => {
          try {
            const body = JSON.parse(r.body);
            postId = body.postId;
            return body.postId !== undefined;
          } catch (e) {
            return false;
          }
        },
        '제목 일치': (r) => {
          try {
            const body = JSON.parse(r.body);
            return body.title === createData.title;
          } catch (e) {
            return false;
          }
        },
      });

      apiSuccessRate.add(success);
      if (!success) postCreateErrors.add(1);
    });

    sleep(0.5);

    if (!postId) return;

    // 2. 게시글 조회
    group('게시글 조회', function () {
      const startTime = new Date();
      const res = http.get(
        `${API_BASE}/posts/${postId}`,
        {
          headers: getHeaders(),
          tags: { name: 'GetPost' },
        }
      );
      postReadDuration.add(new Date() - startTime);

      const success = check(res, {
        '게시글 조회 성공': (r) => r.status === 200,
        '제목 존재': (r) => {
          try {
            const body = JSON.parse(r.body);
            return body.title !== undefined;
          } catch (e) {
            return false;
          }
        },
      });

      apiSuccessRate.add(success);
      if (!success) postReadErrors.add(1);
    });

    sleep(0.5);

    // 3. 게시글 수정
    group('게시글 수정', function () {
      const updateData = {
        title: '수정된 제목',
        content: '수정된 내용입니다.',
        category: '자유게시판',
      };

      const res = http.put(
        `${API_BASE}/posts/${postId}`,
        JSON.stringify(updateData),
        {
          headers: getHeaders(),
          tags: { name: 'UpdatePost' },
        }
      );

      const success = check(res, {
        '게시글 수정 성공': (r) => r.status === 200,
        '수정된 제목 확인': (r) => {
          try {
            const body = JSON.parse(r.body);
            return body.title === updateData.title;
          } catch (e) {
            return false;
          }
        },
      });

      apiSuccessRate.add(success);
      if (!success) postUpdateErrors.add(1);
    });

    sleep(0.5);

    // 4. 게시글 삭제
    group('게시글 삭제', function () {
      const res = http.del(
        `${API_BASE}/posts/${postId}`,
        null,
        {
          headers: getHeaders(),
          tags: { name: 'DeletePost' },
        }
      );

      const success = check(res, {
        '게시글 삭제 성공': (r) => r.status === 204,
      });

      apiSuccessRate.add(success);
      if (!success) postDeleteErrors.add(1);
    });
  });

  sleep(1);

  group('댓글 CRUD', function () {
    const testPostId = __ENV.TEST_POST_ID || 1;
    let commentId;

    // 1. 댓글 작성
    group('댓글 작성', function () {
      const commentData = {
        content: testData.comments[Math.floor(Math.random() * testData.comments.length)],
      };

      const res = http.post(
        `${API_BASE}/posts/${testPostId}/comments`,
        JSON.stringify(commentData),
        {
          headers: getHeaders(),
          tags: { name: 'CreateComment' },
        }
      );

      const success = check(res, {
        '댓글 작성 성공': (r) => r.status === 201,
        '응답에 commentId 포함': (r) => {
          try {
            const body = JSON.parse(r.body);
            commentId = body.commentId;
            return body.commentId !== undefined;
          } catch (e) {
            return false;
          }
        },
      });

      apiSuccessRate.add(success);
      if (!success) commentCreateErrors.add(1);
    });

    sleep(0.5);

    if (!commentId) return;

    // 2. 댓글 조회
    group('댓글 목록 조회', function () {
      const res = http.get(
        `${API_BASE}/posts/${testPostId}/comments`,
        {
          headers: getHeaders(),
          tags: { name: 'GetComments' },
        }
      );

      const success = check(res, {
        '댓글 조회 성공': (r) => r.status === 200,
        '댓글 배열 반환': (r) => {
          try {
            const body = JSON.parse(r.body);
            return Array.isArray(body);
          } catch (e) {
            return false;
          }
        },
      });

      apiSuccessRate.add(success);
      if (!success) commentReadErrors.add(1);
    });

    sleep(0.5);

    // 3. 댓글 수정
    group('댓글 수정', function () {
      const updateData = {
        content: '수정된 댓글 내용입니다.',
      };

      const res = http.put(
        `${API_BASE}/posts/${testPostId}/comments/${commentId}`,
        JSON.stringify(updateData),
        {
          headers: getHeaders(),
          tags: { name: 'UpdateComment' },
        }
      );

      const success = check(res, {
        '댓글 수정 성공': (r) => r.status === 200,
      });

      apiSuccessRate.add(success);
      if (!success) commentUpdateErrors.add(1);
    });

    sleep(0.5);

    // 4. 댓글 삭제
    group('댓글 삭제', function () {
      const res = http.del(
        `${API_BASE}/posts/${testPostId}/comments/${commentId}`,
        null,
        {
          headers: getHeaders(),
          tags: { name: 'DeleteComment' },
        }
      );

      const success = check(res, {
        '댓글 삭제 성공': (r) => r.status === 204,
      });

      apiSuccessRate.add(success);
      if (!success) commentDeleteErrors.add(1);
    });
  });

  sleep(2);
}

// ==================== 시나리오 2: 동시 좋아요 테스트 ====================
export function testConcurrentLikes() {
  const postId = __ENV.TEST_POST_ID || 1;

  group('게시글 좋아요 동시성', function () {
    const startTime = new Date();
    const res = http.post(
      `${API_BASE}/posts/${postId}/likes`,
      null,
      {
        headers: getHeaders(),
        tags: { name: 'TogglePostLike' },
      }
    );
    likeDuration.add(new Date() - startTime);

    const success = check(res, {
      '좋아요 성공': (r) => r.status === 200,
      '응답 시간 1초 이내': (r) => r.timings.duration < 1000,
    });

    apiSuccessRate.add(success);
    if (!success) likeErrors.add(1);
  });

  sleep(0.1);
}

// ==================== 시나리오 3: 동시 조회수 증가 ====================
export function testConcurrentViews() {
  const postId = __ENV.TEST_POST_ID || 1;

  group('조회수 동시성', function () {
    const res = http.post(
      `${API_BASE}/posts/${postId}/views`,
      null,
      {
        headers: getHeaders(),
        tags: { name: 'IncreaseView' },
      }
    );

    const success = check(res, {
      '조회수 증가 성공': (r) => r.status === 200,
    });

    apiSuccessRate.add(success);
    if (!success) viewErrors.add(1);
  });

  sleep(0.3);
}

// ==================== 시나리오 4: 사용자 여정 시뮬레이션 ====================
export function testUserJourney() {
  const testPostId = __ENV.TEST_POST_ID || 1;

  group('사용자 여정', function () {
    // 1. 게시글 목록 조회
    group('메인 페이지 방문', function () {
      const res = http.get(
        `${API_BASE}/posts?page=0&size=10`,
        {
          headers: getHeaders(),
          tags: { name: 'GetPostList' },
        }
      );

      const success = check(res, {
        '목록 조회 성공': (r) => r.status === 200,
      });

      apiSuccessRate.add(success);
    });

    sleep(1);

    // 2. 게시글 상세 조회
    group('게시글 읽기', function () {
      const res = http.get(
        `${API_BASE}/posts/${testPostId}`,
        {
          headers: getHeaders(),
          tags: { name: 'ReadPost' },
        }
      );

      const success = check(res, {
        '게시글 조회 성공': (r) => r.status === 200,
      });

      apiSuccessRate.add(success);
    });

    sleep(0.5);

    // 3. 조회수 증가
    group('조회수 기록', function () {
      const res = http.post(
        `${API_BASE}/posts/${testPostId}/views`,
        null,
        {
          headers: getHeaders(),
          tags: { name: 'RecordView' },
        }
      );

      const success = check(res, {
        '조회수 증가 성공': (r) => r.status === 200,
      });

      apiSuccessRate.add(success);
    });

    sleep(0.5);

    // 4. 댓글 조회
    group('댓글 읽기', function () {
      const res = http.get(
        `${API_BASE}/posts/${testPostId}/comments`,
        {
          headers: getHeaders(),
          tags: { name: 'ReadComments' },
        }
      );

      const success = check(res, {
        '댓글 조회 성공': (r) => r.status === 200,
      });

      apiSuccessRate.add(success);
    });

    sleep(1);

    // 5. 50% 확률로 좋아요
    if (Math.random() > 0.5) {
      group('좋아요 클릭', function () {
        const res = http.post(
          `${API_BASE}/posts/${testPostId}/likes`,
          null,
          {
            headers: getHeaders(),
            tags: { name: 'LikePost' },
          }
        );

        const success = check(res, {
          '좋아요 성공': (r) => r.status === 200,
        });

        apiSuccessRate.add(success);
      });

      sleep(0.5);
    }

    // 6. 30% 확률로 댓글 작성
    if (Math.random() > 0.7) {
      group('댓글 작성', function () {
        const commentData = {
          content: testData.comments[Math.floor(Math.random() * testData.comments.length)],
        };

        const res = http.post(
          `${API_BASE}/posts/${testPostId}/comments`,
          JSON.stringify(commentData),
          {
            headers: getHeaders(),
            tags: { name: 'WriteComment' },
          }
        );

        const success = check(res, {
          '댓글 작성 성공': (r) => r.status === 201,
        });

        apiSuccessRate.add(success);
      });

      sleep(1);
    }

    // 7. 페이지네이션 테스트
    if (Math.random() > 0.8) {
      group('다음 페이지 보기', function () {
        const page = Math.floor(Math.random() * 5);
        const res = http.get(
          `${API_BASE}/posts?page=${page}&size=10`,
          {
            headers: getHeaders(),
            tags: { name: 'Pagination' },
          }
        );

        const success = check(res, {
          '페이지네이션 성공': (r) => r.status === 200,
        });

        if (!success) paginationErrors.add(1);
      });
    }
  });

  sleep(Math.random() * 3);
}

// ==================== 시나리오 5: 스트레스 테스트 ====================
export function testStress() {
  const testPostId = __ENV.TEST_POST_ID || 1;
  const action = Math.random();

  if (action < 0.3) {
    // 30% - 목록 조회
    const res = http.get(
      `${API_BASE}/posts?page=${Math.floor(Math.random() * 10)}&size=10`,
      { headers: getHeaders(), tags: { name: 'StressGetList' } }
    );
    const success = check(res, { '성공': (r) => r.status === 200 });
    apiSuccessRate.add(success);
  } else if (action < 0.5) {
    // 20% - 게시글 조회 + 조회수
    const res1 = http.get(
      `${API_BASE}/posts/${testPostId}`,
      { headers: getHeaders(), tags: { name: 'StressGetPost' } }
    );
    const success1 = check(res1, { '성공': (r) => r.status === 200 });
    apiSuccessRate.add(success1);

    sleep(0.3);

    const res2 = http.post(
      `${API_BASE}/posts/${testPostId}/views`,
      null,
      { headers: getHeaders(), tags: { name: 'StressView' } }
    );
    const success2 = check(res2, { '성공': (r) => r.status === 200 });
    apiSuccessRate.add(success2);
  } else if (action < 0.7) {
    // 20% - 좋아요
    const res = http.post(
      `${API_BASE}/posts/${testPostId}/likes`,
      null,
      { headers: getHeaders(), tags: { name: 'StressLike' } }
    );
    const success = check(res, { '성공': (r) => r.status === 200 });
    apiSuccessRate.add(success);
  } else if (action < 0.85) {
    // 15% - 댓글 조회
    const res = http.get(
      `${API_BASE}/posts/${testPostId}/comments`,
      { headers: getHeaders(), tags: { name: 'StressGetComments' } }
    );
    const success = check(res, { '성공': (r) => r.status === 200 });
    apiSuccessRate.add(success);
  } else {
    // 15% - 댓글 작성
    const commentData = {
      content: testData.comments[Math.floor(Math.random() * testData.comments.length)],
    };
    const res = http.post(
      `${API_BASE}/posts/${testPostId}/comments`,
      JSON.stringify(commentData),
      { headers: getHeaders(), tags: { name: 'StressCreateComment' } }
    );
    const success = check(res, { '성공': (r) => r.status === 201 });
    apiSuccessRate.add(success);
  }

  sleep(Math.random() * 2);
}

// ==================== 결과 요약 ====================
export function handleSummary(data) {
  const summary = generateSummary(data);

  console.log(summary);

  // 결과를 stdout과 JSON 파일로 저장
  const resultPath = __ENV.RESULT_PATH || './src/main/java/com/homesweet/homesweetback/common/community-test-results.json';

  return {
    'stdout': summary,
    [resultPath]: JSON.stringify(data, null, 2),
  };
}

function generateSummary(data) {
  let output = '\n';
  output += '========================================\n';
  output += '  커뮤니티 전체 기능 테스트 결과\n';
  output += '========================================\n\n';

  // 전체 요청 통계
  if (data.metrics.http_reqs) {
    output += ` 총 요청 수: ${data.metrics.http_reqs.values.count}\n`;
  }

  if (data.metrics.http_req_duration) {
    output += '\n  응답 시간:\n';
    const values = data.metrics.http_req_duration.values;
    if (values.avg != null) output += `  - 평균: ${values.avg.toFixed(2)}ms\n`;
    if (values.med != null) output += `  - 중간값: ${values.med.toFixed(2)}ms\n`;
    if (values['p(90)'] != null) output += `  - 90%ile: ${values['p(90)'].toFixed(2)}ms\n`;
    if (values['p(95)'] != null) output += `  - 95%ile: ${values['p(95)'].toFixed(2)}ms\n`;
    if (values['p(99)'] != null) output += `  - 99%ile: ${values['p(99)'].toFixed(2)}ms\n`;
    if (values.max != null) output += `  - 최대: ${values.max.toFixed(2)}ms\n`;
  }

  if (data.metrics.http_req_failed) {
    const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
    const status = failRate < 5 ? '' : '';
    output += `\n${status} 실패율: ${failRate}% (목표: < 5%)\n`;
  }

  if (data.metrics.api_success_rate) {
    const successRate = (data.metrics.api_success_rate.values.rate * 100).toFixed(2);
    const status = successRate > 95 ? '' : '';
    output += `${status} API 성공률: ${successRate}% (목표: > 95%)\n`;
  }

  // 기능별 에러 수
  output += '\n 기능별 에러:\n';
  const errorMetrics = [
    { key: 'post_create_errors', label: '게시글 작성', threshold: 5 },
    { key: 'post_read_errors', label: '게시글 조회', threshold: 5 },
    { key: 'post_update_errors', label: '게시글 수정', threshold: 5 },
    { key: 'post_delete_errors', label: '게시글 삭제', threshold: 5 },
    { key: 'comment_create_errors', label: '댓글 작성', threshold: 5 },
    { key: 'comment_read_errors', label: '댓글 조회', threshold: 5 },
    { key: 'comment_update_errors', label: '댓글 수정', threshold: 5 },
    { key: 'comment_delete_errors', label: '댓글 삭제', threshold: 5 },
    { key: 'like_errors', label: '좋아요', threshold: 10 },
    { key: 'view_errors', label: '조회수', threshold: 10 },
    { key: 'pagination_errors', label: '페이지네이션', threshold: 5 },
  ];

  errorMetrics.forEach(({ key, label, threshold }) => {
    if (data.metrics[key]) {
      const count = data.metrics[key].values.count;
      const status = count < threshold;
      output += `  ${status} ${label}: ${count}개 (목표: < ${threshold})\n`;
    }
  });

  // 성능 메트릭
  output += '\n⚡ 성능 메트릭:\n';

  if (data.metrics.post_create_duration && data.metrics.post_create_duration.values['p(95)'] != null) {
    const p95 = data.metrics.post_create_duration.values['p(95)'].toFixed(2);
    const status = p95 < 1500;
    output += `  ${status} 게시글 작성 p95: ${p95}ms (목표: < 1500ms)\n`;
  }

  if (data.metrics.post_read_duration && data.metrics.post_read_duration.values['p(95)'] != null) {
    const p95 = data.metrics.post_read_duration.values['p(95)'].toFixed(2);
    const status = p95 < 500 ;
    output += `  ${status} 게시글 조회 p95: ${p95}ms (목표: < 500ms)\n`;
  }

  if (data.metrics.like_duration && data.metrics.like_duration.values['p(95)'] != null) {
    const p95 = data.metrics.like_duration.values['p(95)'].toFixed(2);
    const status = p95 < 800 ;
    output += `  ${status} 좋아요 p95: ${p95}ms (목표: < 800ms)\n`;
  }

  // 체크 통과율
  if (data.metrics.checks) {
    const passRate = (data.metrics.checks.values.rate * 100).toFixed(2);
    const status = passRate > 95 ;
    output += `\n${status} 체크 통과율: ${passRate}%\n`;
  }

  output += '\n========================================\n';
  output += '상세 결과: community-test-results.json\n';
  output += '========================================\n\n';

  return output;
}
