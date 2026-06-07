# --- ETAPA 1 : Build (La construccion)
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' mvnw
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# --- Etapa 2: RUNTIME (el producto final)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8075
ENTRYPOINT ["java", "-jar", "app.jar"]
