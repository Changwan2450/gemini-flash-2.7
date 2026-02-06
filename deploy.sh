#!/bin/bash

# 1. 자바 빌드
echo "🔨 자바 빌드 중..."
./mvnw clean package -DskipTests

# 2. 도커 이미지 생성 (이름: gemini-bot)
echo "🐳 도커 이미지 굽는 중..."
docker build -t gemini-bot .

# 3. 기존 컨테이너 삭제
echo "🚀 기존 컨테이너 삭제 중..."
docker rm -f my-bot

# 4. 신규 실행 (이미지 이름 gemini-bot 으로 수정)
echo "🚀 신규 컨테이너 실행 중..."
docker run -d \
  --name my-bot \
  -v /Users:/host_root \
  -e TZ=Asia/Seoul \
  -p 8080:8080 \
  gemini-bot

echo "✅ 배포 완료! 이제 1분만 기다려봐 형."