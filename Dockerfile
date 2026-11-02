FROM node:22-alpine AS ui
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=ui /src/backend/src/main/resources/static backend/src/main/resources/static
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/backend/target/acme-compensation.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
