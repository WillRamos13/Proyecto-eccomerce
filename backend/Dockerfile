# Build desde la raíz del repositorio:
# docker build -f backend/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/backend

# Cachea dependencias antes de copiar el código.
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline

# El backend y el frontend se empaquetan en un único JAR para producción.
COPY backend/src ./src
COPY frontend ./src/main/resources/static

# Las pruebas unitarias forman parte del build de producción.
RUN mvn -B -q clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system fastmarket \
    && useradd --system --gid fastmarket --home-dir /app --shell /usr/sbin/nologin fastmarket

COPY --from=build --chown=fastmarket:fastmarket /workspace/backend/target/*.jar /app/app.jar

USER fastmarket
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
