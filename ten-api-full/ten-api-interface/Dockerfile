# ===== ten-api-interface Dockerfile =====
FROM openjdk:8-jdk-alpine

LABEL maintainer="ten-api-platform"

RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

COPY target/ten-api-interface-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8123

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
