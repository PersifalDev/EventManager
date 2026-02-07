
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY event-manager/pom.xml ./event-manager/pom.xml
COPY event-notificator/pom.xml ./event-notificator/pom.xml
COPY common-libs/pom.xml ./common-libs/pom.xml

RUN mvn -q -B -DskipTests dependency:go-offline

COPY common-libs ./common-libs
COPY event-manager ./event-manager

RUN mvn -q -B -DskipTests -pl event-manager -am package

RUN set -eux; \
    ls -la event-manager/target; \
    JAR="$(ls -1 event-manager/target/*.jar | grep -vE '(original-|sources|javadoc)' | head -n 1)"; \
    echo "Using JAR: $JAR"; \
    cp "$JAR" /workspace/app.jar; \
    echo "=== MANIFEST ==="; \
    unzip -p /workspace/app.jar META-INF/MANIFEST.MF | sed -n '1,120p' || true; \
    echo "=== BOOT-INF CHECK ==="; \
    jar tf /workspace/app.jar | grep -E '^BOOT-INF/' | head -n 5 || true

FROM bellsoft/liberica-openjre-alpine:21-cds
WORKDIR /application

RUN adduser -S spring-user
USER spring-user

COPY --from=build /workspace/app.jar app.jar

ENTRYPOINT ["java","-jar","/application/app.jar"]
