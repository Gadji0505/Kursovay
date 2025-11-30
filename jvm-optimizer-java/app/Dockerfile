# app/Dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY TestApp.java .
RUN javac TestApp.java
ENV JAVA_OPTS=""
# Запуск: JAVA_OPTS будет содержать -Xms... -Xmx...
CMD ["sh", "-c", "java $JAVA_OPTS TestApp"]
