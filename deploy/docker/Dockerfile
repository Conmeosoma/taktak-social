# Dockerfile cho Taktak Spring Boot Server (Multi-stage build)
FROM eclipse-temurin:17-jre-alpine AS runtime

# Tạo user dịch vụ không phải root
RUN addgroup -S taktak && adduser -S taktak -G taktak

# Thư mục ứng dụng và thư mục lưu trữ ảnh riêng tư
WORKDIR /app
RUN mkdir -p /var/taktak/private-images && chown -R taktak:taktak /app /var/taktak

# Copy file jar executable
ARG JAR_FILE=server.jar
COPY ${JAR_FILE} /app/taktak-server.jar
RUN chown taktak:taktak /app/taktak-server.jar

USER taktak

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Dspring.profiles.active=prod", "-jar", "/app/taktak-server.jar"]
