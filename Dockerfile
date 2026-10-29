# =========================
# Stage 1 - Build da aplicação
# =========================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

# =========================
# Stage 2 - Runtime enxuto com JRE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria grupo e usuário não-root obrigatório (uid 1001)
RUN addgroup -S spring && adduser -S springuser -u 1001 -G spring

# Copia o JAR gerado no estágio de build
COPY --from=builder /build/target/*.jar /app/app.jar

# Ajusta permissões para o usuário não-root
RUN chown -R springuser:spring /app

EXPOSE 8080

# Define usuário não-root antes do ENTRYPOINT
USER springuser

# Usa até 75% da memória do contêiner (o Cloud Run limita a RAM; sem isso a JVM pode estourar e ser morta).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]