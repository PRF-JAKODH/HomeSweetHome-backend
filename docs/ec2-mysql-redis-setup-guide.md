# EC2에 MySQL/Redis 서버 설치 가이드

AWS EC2 인스턴스에 MySQL과 Redis를 설치하고 외부 접속 가능하게 설정하는 방법을 정리합니다.

## 아키텍처

```
[Backend EC2]  ──────►  [DB EC2 - MySQL]
     │
     └───────────────►  [Redis EC2 - Redis]
```

---

## EC2 인스턴스 생성

### 공통 설정

| 항목 | 값 |
|------|-----|
| AMI | Ubuntu Server 24.04 LTS |
| 인스턴스 유형 | t3.micro |
| 키 페어 | 기존 키 재사용 가능 |

### 보안 그룹 설정

**DB 서버 (homesweet-db-sg)**

| 유형 | 포트 | 소스 | 설명 |
|------|------|------|------|
| SSH | 22 | 내 IP | 관리자 접속 |
| MySQL/Aurora | 3306 | Backend 프라이빗 IP/32 | 앱 DB 접속 |

**Redis 서버 (homesweet-redis-sg)**

| 유형 | 포트 | 소스 | 설명 |
|------|------|------|------|
| SSH | 22 | 내 IP | 관리자 접속 |
| 사용자 지정 TCP | 6379 | Backend 프라이빗 IP/32 | 앱 Redis 접속 |

---

## MySQL 서버 설치

### SSH 접속

```bash
ssh -i ~/.ssh/key.pem ubuntu@<DB_퍼블릭_IP>
```

### 설치 및 설정

```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# MySQL 설치
sudo apt install -y mysql-server

# 외부 접속 허용 설정
sudo sed -i 's/bind-address.*=.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf

# MySQL 재시작
sudo systemctl restart mysql
```

### 데이터베이스 및 사용자 생성

```bash
sudo mysql
```

```sql
-- 데이터베이스 생성
CREATE DATABASE homesweet;

-- 사용자 생성 (외부 접속 허용)
CREATE USER 'homesweet'@'%' IDENTIFIED BY 'your_password';

-- 권한 부여
GRANT ALL PRIVILEGES ON homesweet.* TO 'homesweet'@'%';

-- 즉시 적용
FLUSH PRIVILEGES;

EXIT;
```

### 설정 확인

```bash
# MySQL 상태 확인
sudo systemctl status mysql

# 외부 접속 테스트 (Backend 서버에서)
mysql -h <DB_프라이빗_IP> -u homesweet -p
```

---

## Redis 서버 설치

### SSH 접속

```bash
ssh -i ~/.ssh/key.pem ubuntu@<Redis_퍼블릭_IP>
```

### 설치 및 설정

```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Redis 설치
sudo apt install -y redis-server

# 외부 접속 허용 설정
sudo sed -i 's/bind 127.0.0.1/bind 0.0.0.0/' /etc/redis/redis.conf
sudo sed -i 's/protected-mode yes/protected-mode no/' /etc/redis/redis.conf

# Redis 재시작
sudo systemctl restart redis-server
```

### 비밀번호 설정 (선택)

```bash
sudo vi /etc/redis/redis.conf
```

`requirepass` 찾아서 주석 해제 후 비밀번호 설정:
```
requirepass your_redis_password
```

```bash
sudo systemctl restart redis-server
```

### 설정 확인

```bash
# Redis 상태 확인
sudo systemctl status redis-server

# 로컬 테스트
redis-cli ping
# 응답: PONG

# 외부 접속 테스트 (Backend 서버에서)
redis-cli -h <Redis_프라이빗_IP> ping
```

---

## Backend 환경 변수 설정

Backend 서버의 `.env` 파일 수정:

```bash
# DB 설정
DATABASE_HOST=<DB_프라이빗_IP>
DATABASE_URL=jdbc:mysql://<DB_프라이빗_IP>:3306/homesweet
DATABASE_USERNAME=homesweet
DATABASE_PASSWORD=your_password

# Redis 설정
REDIS_HOST=<Redis_프라이빗_IP>
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password  # 설정한 경우
```

---

## 주요 명령어 요약

### MySQL

| 명령어 | 설명 |
|--------|------|
| `sudo systemctl status mysql` | 상태 확인 |
| `sudo systemctl restart mysql` | 재시작 |
| `sudo mysql` | MySQL 접속 |
| `sudo vi /etc/mysql/mysql.conf.d/mysqld.cnf` | 설정 파일 편집 |

### Redis

| 명령어 | 설명 |
|--------|------|
| `sudo systemctl status redis-server` | 상태 확인 |
| `sudo systemctl restart redis-server` | 재시작 |
| `redis-cli` | Redis CLI 접속 |
| `sudo vi /etc/redis/redis.conf` | 설정 파일 편집 |

---

## 트러블슈팅

### 연결 안 될 때 체크리스트

1. **보안 그룹** - 포트가 열려있는지 확인
2. **서비스 상태** - `systemctl status` 확인
3. **bind 설정** - `0.0.0.0`으로 되어있는지 확인
4. **IP 주소** - 프라이빗 IP를 사용하고 있는지 확인

### 로그 확인

```bash
# MySQL 로그
sudo tail -f /var/log/mysql/error.log

# Redis 로그
sudo tail -f /var/log/redis/redis-server.log
```
