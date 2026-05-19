# --- ETAPA 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -Dmaven.test.skip=true

# --- ETAPA 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar product.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "product.jar"]
