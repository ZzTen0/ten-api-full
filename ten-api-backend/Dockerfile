# ===== ten-api-backend Dockerfile =====
# 基础镜像：JDK 8
FROM openjdk:8-jdk-alpine

LABEL maintainer="ten-api-platform"

# 时区
RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 拷贝已打好的 jar 包（请先在宿主机器执行：mvn clean package -DskipTests）
COPY target/ten-api-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 7529 20880

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
