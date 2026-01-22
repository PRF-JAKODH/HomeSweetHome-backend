# 로컬 Docker 이미지를 EC2에 배포하기 (Git Clone 없이)

로컬에서 Docker 이미지를 빌드하고 EC2에 배포하는 방법을 정리합니다.
Git Clone 방식보다 빠르고 효율적인 배포가 가능합니다.

## 배포 흐름

```
[로컬 Mac]                    [Docker Hub]              [EC2 서버]
    │                              │                        │
    │ docker buildx build          │                        │
    │ --push                       │                        │
    ├─────────────────────────────►│                        │
    │                              │                        │
    │                              │   docker pull          │
    │                              │◄───────────────────────┤
    │                              │                        │
    │                              │   이미지 전송           │
    │                              ├───────────────────────►│
    │                              │                        │
    │                              │                  docker run
```

## 사전 준비

### 1. Docker Hub 계정
- https://hub.docker.com 에서 계정 생성
- 로컬에서 로그인: `docker login`

### 2. EC2 인스턴스
- Docker 설치 완료
- 보안 그룹에서 8080 포트 허용

---

## 배포 단계

### Step 1: 로컬에서 이미지 빌드 및 푸시

```bash
cd /path/to/project

docker buildx build \
  --platform linux/amd64 \
  -t <dockerhub-username>/<image-name>:<tag> \
  --push \
  --no-cache \
  .
```

**옵션 설명:**

| 옵션 | 설명 |
|------|------|
| `--platform linux/amd64` | EC2 아키텍처에 맞게 빌드 (Apple Silicon Mac 필수) |
| `-t` | 이미지 이름과 태그 지정 |
| `--push` | 빌드 완료 후 Docker Hub에 자동 푸시 |
| `--no-cache` | 캐시 없이 처음부터 빌드 |

**예시:**
```bash
docker buildx build \
  --platform linux/amd64 \
  -t ohhalim/homesweet-commu:1 \
  --push \
  --no-cache \
  .
```

### Step 2: EC2에서 이미지 Pull 및 실행

```bash
# SSH 접속
ssh -i ~/.ssh/key.pem ubuntu@<EC2-IP>

# 기존 컨테이너 정리 (있는 경우)
docker stop homesweet-backend
docker rm homesweet-backend

# 이미지 다운로드
docker pull <dockerhub-username>/<image-name>:<tag>

# 컨테이너 실행
docker run -d \
  --name homesweet-backend \
  -p 8080:8080 \
  --env-file .env \
  <dockerhub-username>/<image-name>:<tag>
```

**예시:**
```bash
docker pull ohhalim/homesweet-commu:1

docker run -d \
  --name homesweet-backend \
  -p 8080:8080 \
  --env-file .env \
  ohhalim/homesweet-commu:1
```

---

## Docker Compose로 실행하기

`docker-compose.yml` 작성:

```yaml
services:
  spring:
    image: ohhalim/homesweet-commu:1
    container_name: homesweet-backend
    env_file:
      - .env
    ports:
      - "8080:8080"
    restart: always
```

실행:
```bash
docker compose pull
docker compose up -d
```

---

## 주의사항

### Apple Silicon Mac 사용 시
반드시 `--platform linux/amd64` 옵션 필요.
없으면 EC2에서 `exec format error` 발생.

### 환경 변수
EC2에 `.env` 파일 미리 준비 필요:
```bash
# EC2에서
cat > .env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:mysql://<DB_IP>:3306/homesweet
REDIS_HOST=<REDIS_IP>
# ... 기타 환경 변수
EOF
```

---

## Git Clone 방식과 비교

| 항목 | Docker Hub 방식 | Git Clone 방식 |
|------|----------------|----------------|
| 빌드 위치 | 로컬 | EC2 서버 |
| 배포 속도 | 빠름 (이미지만 전송) | 느림 (빌드 시간 포함) |
| 서버 리소스 | 적음 | 많음 (빌드 중 메모리/CPU 사용) |
| 코드 노출 | 없음 (이미지만 존재) | 있음 (소스코드 존재) |

---

## 배포 자동화 스크립트

로컬에서 한 번에 실행할 수 있는 스크립트:

```bash
#!/bin/bash
# deploy.sh

VERSION=$1
IMAGE_NAME="ohhalim/homesweet-commu"
EC2_HOST="ubuntu@<EC2-IP>"
KEY_PATH="~/.ssh/key.pem"

if [ -z "$VERSION" ]; then
  echo "Usage: ./deploy.sh <version>"
  exit 1
fi

echo "=== Building and pushing image ==="
docker buildx build \
  --platform linux/amd64 \
  -t $IMAGE_NAME:$VERSION \
  --push \
  --no-cache \
  .

echo "=== Deploying to EC2 ==="
ssh -i $KEY_PATH $EC2_HOST << EOF
  docker pull $IMAGE_NAME:$VERSION
  docker stop homesweet-backend || true
  docker rm homesweet-backend || true
  docker run -d \
    --name homesweet-backend \
    -p 8080:8080 \
    --env-file .env \
    $IMAGE_NAME:$VERSION
EOF

echo "=== Deployment complete ==="
```

사용:
```bash
chmod +x deploy.sh
./deploy.sh 1.0.0
```
