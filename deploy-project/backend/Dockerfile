FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S resumematch \
    && adduser -S resumematch -G resumematch

COPY backend-0.0.1-SNAPSHOT.jar app.jar

USER resumematch

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
