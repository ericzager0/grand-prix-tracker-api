# ==========================================
# Etapa 1: Build con Maven y Eclipse Temurin 25
# ==========================================
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copiar configuración de dependencias primero para cachear capas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el JAR ejecutable
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Runtime liviano con JRE 25
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

# Optimización estricta de memoria para el plan Free de Render (512 MB RAM):
# -XX:+UseSerialGC: Recolector de basura serial, mínimo overhead de memoria y threads.
# -Xss512k: Reduce el tamaño de stack por thread a la mitad.
# -XX:MaxRAMPercentage=75: Limita el heap al ~75% de la memoria del contenedor (máx ~384MB).
ENV JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75"

# Puerto por defecto de Render
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

