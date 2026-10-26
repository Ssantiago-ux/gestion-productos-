# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Etapa 2: ejecutar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/gestion-productos.jar app.jar
ENV DB_PATH=/data/productos.db
RUN mkdir -p /data
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
