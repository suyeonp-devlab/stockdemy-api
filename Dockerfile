# syntax=docker/dockerfile:1

# ===== builder: 실행 jar 빌드 단계 =====
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# 빌드 설정만 먼저 복사해 의존성 다운로드 레이어를 캐시
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# EC2(1GB)에서 빌드하므로 Gradle 메모리와 병렬 작업 수를 제한
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.jvmargs=-Xmx512m -Dorg.gradle.workers.max=1"
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

# 소스코드 복사 후 실행 jar 생성 (테스트는 Testcontainers가 도커를 요구해 이미지 빌드에서 돌리지 않음)
COPY src ./src
RUN ./gradlew bootJar --no-daemon && cp build/libs/*.jar app.jar

# ===== runner: 실제로 컨테이너가 실행될 때 쓰이는 최종 이미지 =====
FROM eclipse-temurin:25-jre
WORKDIR /app

ENV TZ=Asia/Seoul

# 실행 전용 사용자 + 로그 디렉터리 (logback이 ./logs에 파일 로그를 남김)
RUN groupadd --system spring \
  && useradd --system --gid spring spring \
  && mkdir -p /app/logs \
  && chown spring:spring /app/logs

COPY --from=builder --chown=spring:spring /workspace/app.jar ./app.jar

USER spring
EXPOSE 8080

# 메모리 옵션은 배포 환경에서 JAVA_OPTS로 주입
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
