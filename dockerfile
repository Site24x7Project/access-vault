# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src/app

# Your Dockerfile is inside the module folder (same place as pom.xml and src/)
COPY pom.xml .
COPY src ./src

# Use Maven cache & build
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
ENV TZ=UTC
WORKDIR /app

# create non-root user
RUN useradd -m appuser

# create and own runtime dirs as root, then switch user
RUN mkdir -p /app/exports /app/logs && chown -R appuser:appuser /app
USER appuser

# copy fat JAR from build stage
COPY --from=build /src/app/target/accessvault-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8089
ENTRYPOINT ["java","-jar","/app/app.jar"]
