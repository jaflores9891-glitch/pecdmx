# Receta de la "caja" del backend. Tiene dos etapas para que la imagen final sea pequeña:
# la primera compila (necesita el JDK completo y Maven) y la segunda solo corre (le basta el JRE).

# --- Etapa 1: compilar ---
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app

# Primero solo el pom: si no cambian las dependencias, Docker reutiliza esta capa y no las vuelve a descargar.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
# Los tests ya corrieron en la CI (y aquí no hay Docker para Testcontainers), por eso se saltan.
RUN ./mvnw -B -ntp package -DskipTests

# --- Etapa 2: correr ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# Nunca correr como administrador (root): si alguien encontrara una falla, tendría menos poder.
RUN useradd --system --no-create-home pecdmx
COPY --from=compilacion /app/target/backend-*.jar app.jar
USER pecdmx

# Ajustes para caber en los 512 MB del plan gratis de Render:
#  - MaxRAMPercentage: Java usa como máximo el 70 % de la memoria del contenedor (deja aire para lo demás).
#  - UseSerialGC: el recolector de basura más ligero; con 0.1 de CPU no conviene uno que use varios hilos.
#  - TieredStopAtLevel=1: compila menos agresivamente; arranca más rápido y usa menos memoria.
#  - Xss512k: menos memoria por cada hilo.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
