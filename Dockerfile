FROM eclipse-temurin:21-jre

LABEL description="Backend Application - Image"

WORKDIR /app

# 보안을 위한 non-root 유저 생성
RUN groupadd -r spring && useradd -r -g spring spring

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# CI에서 빌드한 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R spring:spring /app

USER spring

# 최적화 JVM 옵션
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
  
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]