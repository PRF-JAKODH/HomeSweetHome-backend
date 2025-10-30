#!/bin/bash

set -euo pipefail

# ===== Config =====
# 사설망 CIDR (예: 가정/사무실 일반 환경)
# 필요 시 10.0.0.0/8, 172.16.0.0/12 등으로 변경하세요
CIDR="172.16.0.0/12"
PORTS=("9100" "9104" "8080")
BACKUP="/etc/pf.conf.backup.$(date +%Y%m%d%H%M%S)"

# ===== UI Helpers =====
BLUE="\033[1;34m"
GREEN="\033[1;32m"
YELLOW="\033[1;33m"
RED="\033[1;31m"
GRAY="\033[0;37m"
NC="\033[0m"

CHECK="✅"
ARROW="➜"
WARN="⚠️"
CROSS="❌"

section() {
  echo ""
  echo -e "${BLUE}==== $1 ====${NC}"
}

info() {
  echo -e "${GRAY}${ARROW} $1${NC}"
}

success() {
  echo -e "${GREEN}${CHECK} $1${NC}"
}

warn() {
  echo -e "${YELLOW}${WARN} $1${NC}"
}

error_exit() {
  echo -e "${RED}${CROSS} $1${NC}"
  exit 1
}

section "PF 방화벽 규칙 적용 (CIDR: ${CIDR}, Ports: ${PORTS[*]})"

section "백업"
info "/etc/pf.conf -> ${BACKUP}"
sudo cp /etc/pf.conf "${BACKUP}"
success "백업 완료"

section "룰 삽입 여부 확인"
info "포트별로 필요한 규칙만 추가합니다"

MISSING_RULES=""
for port in "${PORTS[@]}"; do
  # allow rule: pass from CIDR to port
  if ! grep -qE "^pass in proto tcp from ${CIDR} to any port ${port}(\s|$)" /etc/pf.conf; then
    MISSING_RULES+=$'pass in proto tcp from '${CIDR}' to any port '${port}' # monitoring allow local network\n'
  fi
  # block rule: block external to port
  # 내부망 외의 소스만 차단하도록 from ! CIDR 사용
  if ! grep -qE "^block in proto tcp from ! ${CIDR} to any port ${port}(\s|$)" /etc/pf.conf; then
    MISSING_RULES+=$'block in proto tcp from ! '${CIDR}' to any port '${port}' # monitoring block external (not in CIDR)\n'
  fi
done

if [[ -n "${MISSING_RULES}" ]]; then
  # 루프백(127.0.0.1) 예외: localhost 접근은 항상 허용
  SKIP_LO0=""
  if ! grep -qE "^set skip on lo0(\s|$)" /etc/pf.conf; then
    SKIP_LO0=$'set skip on lo0\n'
  fi
  info "부족한 규칙을 /etc/pf.conf에 추가합니다"
  sudo bash -c "cat >> /etc/pf.conf" <<EOF

# monitoring access control (Added by monitoring.sh)
${SKIP_LO0%$'\n'}
${MISSING_RULES%$'\n'}
# end of monitoring rules
EOF
  success "부족한 규칙 추가 완료"
else
  warn "추가할 규칙이 없습니다. 모든 포트 규칙이 이미 존재합니다."
fi

section "pf.conf 문법 검사"
if ! sudo pfctl -nf /etc/pf.conf; then
  warn "문법 오류 감지. 백업본으로 복구합니다: ${BACKUP}"
  sudo cp "${BACKUP}" /etc/pf.conf || error_exit "백업 복구 실패"
  exit 1
fi
success "문법 정상"

section "룰 적용 및 PF 활성화"
info "pfctl -f /etc/pf.conf"
sudo pfctl -f /etc/pf.conf
info "pfctl -e (이미 활성화되어 있으면 무시됨)"
sudo pfctl -e || true
success "PF 적용/활성화 완료"

section "적용 결과 확인"
info "9100/9104/8080 관련 룰 요약"
sudo pfctl -sr | egrep '9100|9104|8080' || true
success "완료"