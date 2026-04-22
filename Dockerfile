# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to leverage Docker cache
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Tesseract OCR
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/phinder-1.0.0-SNAPSHOT.jar phinder.jar
# Since Phileas might need some local storage or cache, we ensure /tmp is writable
VOLUME /tmp
ENTRYPOINT ["java", "-jar", "phinder.jar"]
