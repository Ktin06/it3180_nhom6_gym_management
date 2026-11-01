# Ảnh Docker của backend để triển khai Render. Build: docker build -t gym-backend ./backend

# ---- Giai đoạn 1: build bằng Maven wrapper và JDK 21 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Tải thư viện trước để tận dụng cache của Docker khi chỉ mã nguồn thay đổi.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Kiểm thử đã chạy trong CI (cần Docker cho Testcontainers), nên bỏ qua ở bước đóng gói.
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# ---- Giai đoạn 2: chạy bằng JRE 21, người dùng không phải root ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=staging \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
