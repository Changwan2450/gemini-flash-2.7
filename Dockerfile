# 1. 자바 21 환경 빌드용 (가벼운 Alpine 버전)
FROM eclipse-temurin:21-jdk-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 jar 파일 복사 (target 폴더에 있는 거)
COPY target/*.jar app.jar

# 4. 실행 (메모리 제한 형 스타일로 추가)
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]