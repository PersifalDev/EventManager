
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml

COPY event-manager/pom.xml ./event-manager/pom.xml
COPY event-notificator/pom.xml ./event-notificator/pom.xml
COPY common-libs/pom.xml ./common-libs/pom.xml

RUN mvn -q -B -DskipTests dependency:go-offline


COPY common-libs ./common-libs
COPY event-notificator ./event-notificator


RUN mvn -q -B -DskipTests -pl event-notificator -am package

RUN set -eux; \
    ls -la event-notificator/target; \
    JAR="$(ls -1 event-notificator/target/*.jar | grep -vE '(original-|sources|javadoc)' | head -n 1)"; \
    echo "Using JAR: $JAR"; \
    cp "$JAR" /workspace/app.jar



FROM bellsoft/liberica-openjre-alpine:21-cds AS layers
WORKDIR /application

COPY --from=build /workspace/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted


FROM bellsoft/liberica-openjre-alpine:21-cds

VOLUME /tmp

RUN adduser -S spring-user
USER spring-user
WORKDIR /application

COPY --from=layers /application/app.jar app.jar

COPY --from=layers /application/extracted/dependencies/ ./
COPY --from=layers /application/extracted/spring-boot-loader/ ./
COPY --from=layers /application/extracted/snapshot-dependencies/ ./
COPY --from=layers /application/extracted/application/ ./

RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=app.jsa -Xlog:class+load:file=/tmp/classload.log"
ENV JAVA_ERROR_FILE_OPTS="-XX:ErrorFile=/tmp/java_error.log"
ENV JAVA_HEAP_DUMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"
ENV JAVA_ON_OUT_OF_MEMORY_OPTS="-XX:+ExitOnOutOfMemoryError"
ENV JAVA_NATIVE_MEMORY_TRACKING_OPTS="-XX:NativeMemoryTracking=summary -XX:+UnlockDiagnosticVMOptions -XX:+PrintNMTStatistics"

ENTRYPOINT ["sh","-c","java $JAVA_HEAP_DUMP_OPTS $JAVA_ON_OUT_OF_MEMORY_OPTS $JAVA_ERROR_FILE_OPTS $JAVA_NATIVE_MEMORY_TRACKING_OPTS $JAVA_CDS_OPTS -jar app.jar"]
