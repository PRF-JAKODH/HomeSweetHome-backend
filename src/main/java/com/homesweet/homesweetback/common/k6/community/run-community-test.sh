#!/bin/bash

# 커뮤니티 전체 기능 테스트 실행 스크립트
# 사용법: ./run-community-test.sh

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 기본값 설정
BASE_URL=${BASE_URL:-"http://localhost:8080"}
TEST_POST_ID=${TEST_POST_ID:-1}
AUTH_TOKEN=${AUTH_TOKEN:-""}

# 헬퍼 함수
print_header() {
    echo -e "\n${CYAN}========================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# 애플리케이션 실행 확인
check_app_running() {
    print_info "애플리케이션 실행 확인 중..."

    if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        print_success "애플리케이션 실행 중 (${BASE_URL})"
        return 0
    else
        print_error "애플리케이션이 실행되지 않았습니다."
        echo ""
        echo "애플리케이션을 먼저 실행하세요:"
        echo "  ${YELLOW}./gradlew bootRun${NC}"
        echo ""
        return 1
    fi
}

# k6 설치 확인
check_k6_installed() {
    print_info "k6 설치 확인 중..."

    if command -v k6 &> /dev/null; then
        K6_VERSION=$(k6 version | head -n 1)
        print_success "k6 설치됨 (${K6_VERSION})"
        return 0
    else
        print_error "k6가 설치되지 않았습니다."
        echo ""
        echo "k6 설치 방법:"
        echo "  ${YELLOW}brew install k6${NC}  # macOS"
        echo ""
        return 1
    fi
}

# 테스트 데이터 확인
check_test_data() {
    print_info "테스트 데이터 확인 중..."

    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/community/posts/${TEST_POST_ID}")

    if [ "$RESPONSE" = "200" ]; then
        print_success "테스트 게시글 존재 (ID: ${TEST_POST_ID})"
        return 0
    else
        print_warning "테스트 게시글을 찾을 수 없습니다 (ID: ${TEST_POST_ID})"
        print_info "일부 테스트는 기본 게시글이 필요합니다."
        return 0
    fi
}

# 테스트 정보 출력
print_test_info() {
    print_header "커뮤니티 전체 기능 테스트"

    echo -e "${BLUE}테스트 범위:${NC}"
    echo "  ✅ 게시글 CRUD (작성, 조회, 수정, 삭제)"
    echo "  ✅ 댓글 CRUD (작성, 조회, 수정, 삭제)"
    echo "  ✅ 게시글 좋아요 토글 & 상태 확인"
    echo "  ✅ 댓글 좋아요 토글 & 상태 확인"
    echo "  ✅ 조회수 증가"
    echo "  ✅ 페이지네이션"
    echo "  ✅ 동시성 제어 검증"
    echo ""

    echo -e "${BLUE}테스트 시나리오:${NC}"
    echo "  1. CRUD 작업 (2분) - 기본 생성/조회/수정/삭제"
    echo "  2. 동시 좋아요 (1분) - 100명이 각각 10번씩 좋아요 (총 1000회)"
    echo "  3. 동시 조회수 (30초) - 50명이 동시에 조회"
    echo "  4. 사용자 여정 (3분) - 실제 사용 패턴 시뮬레이션"
    echo "  5. 스트레스 테스트 (7분) - 최대 300명까지 부하"
    echo ""
    echo -e "${YELLOW}총 예상 시간: 약 13분${NC}"
    echo ""
}

# 설정 정보 출력
print_settings() {
    echo -e "${BLUE}설정 정보:${NC}"
    echo "  BASE_URL: ${BASE_URL}"
    echo "  TEST_POST_ID: ${TEST_POST_ID}"
    if [ -n "$AUTH_TOKEN" ]; then
        echo "  AUTH_TOKEN: 설정됨 ✓"
    else
        echo "  AUTH_TOKEN: ${YELLOW}미설정 (선택사항)${NC}"
    fi
    echo ""
}

# 테스트 실행
run_test() {
    print_header "테스트 시작"

    k6 run \
        -e BASE_URL="${BASE_URL}" \
        -e TEST_POST_ID="${TEST_POST_ID}" \
        -e AUTH_TOKEN="${AUTH_TOKEN}" \
        "$(dirname "$0")/community-full-test.js"

    local exit_code=$?

    if [ $exit_code -eq 0 ]; then
        print_header "테스트 완료 ✅"
        echo ""
        print_success "모든 테스트가 성공적으로 완료되었습니다!"
        echo ""
        print_info "상세 결과 파일: src/main/java/com/homesweet/homesweetback/common/community-test-results.json"
        echo ""
        print_info "동시성 제어 검증을 위해 데이터베이스를 확인하세요:"
        echo ""
        echo "  ${CYAN}SELECT post_id, like_count FROM community_posts WHERE post_id = ${TEST_POST_ID};${NC}"
        echo "  ${CYAN}SELECT COUNT(*) FROM community_post_likes WHERE post_id = ${TEST_POST_ID};${NC}"
        echo ""
        echo "  ✅ 두 값이 일치하면 동시성 제어 성공!"
        echo ""
    else
        print_header "테스트 실패 ❌"
        echo ""
        print_error "일부 테스트가 실패했습니다."
        echo ""
        print_info "상세 로그를 확인하여 문제를 파악하세요."
        echo ""
        exit 1
    fi
}

# 메인 함수
main() {
    clear

    print_header "커뮤니티 전체 기능 k6 부하테스트"

    # 사전 검사
    check_k6_installed || exit 1
    echo ""
    check_app_running || exit 1
    echo ""
    check_test_data
    echo ""

    # 테스트 정보
    print_test_info
    print_settings

    # 확인
    echo -e "${YELLOW}테스트를 시작하시겠습니까? (y/N)${NC}"
    read -r response

    if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_warning "테스트가 취소되었습니다."
        exit 0
    fi

    # 테스트 실행
    run_test
}

# 스크립트 실행
main "$@"
