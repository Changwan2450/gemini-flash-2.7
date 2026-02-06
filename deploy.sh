#!/bin/bash

# 1. 자바 빌드 (테스트는 시간 걸리니까 스킵)
echo "🔨 자바 빌드 중..."
./mvnw clean package -DskipTests

# 2. 도커 이미지 생성 (이름: gemini-bot)
echo "🐳 도커 이미지 굽는 중..."
docker build -t gemini-bot .

# 3. 기존 컨테이너 삭제 및 신규 실행
echo "🚀 컨테이너 교체 중..."
docker rm -f my-bot

# [중요] / 대신 /Users로 마운트해야 맥 OS 보안 통과함!
docker run -d -p 8081:8080 --name my-bot -v /Users:/host_root:ro gemini-bot

echo "✅ 배포 완료! 이제 텔레그램 확인해봐 형."