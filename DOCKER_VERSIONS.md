# Docker 배포 버전 히스토리

이 파일은 HomeSweetHome 프로젝트의 Docker 이미지 배포 이력을 기록합니다.

| 날짜 | 시간 | 이미지 태그 | 빌드 명령 | 플랫폼 | 설명 |
| 2025-11-06 | 14:28:27 | `jakodh/homesweethome-backend:V1.0.6` | `docker buildx build --platform linux/amd64 -t jakodh/homesweethome-backend:V1.0.6 --push .` | linux/amd64 | Manual local deployment |
