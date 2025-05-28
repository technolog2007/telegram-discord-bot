# syntax=docker/dockerfile:1.4
# Обов'язково вкажи цю директиву на першому рядку для використання BuildKit secrets.

# --- Етап збірки (Builder stage) ---
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Встановлюємо робочу директорію всередині контейнера
WORKDIR /app

# Оголошуємо ARG для токена GitHub.
ARG GITHUB_TOKEN

# Копіюємо pom.xml та src/ для оптимізації кешування Maven
COPY pom.xml .
COPY src ./src

# Створюємо тимчасовий файл settings.xml всередині контейнера
RUN mkdir -p ~/.m2 && \
    echo "<settings>" >> ~/.m2/settings.xml && \
    echo "  <servers>" >> ~/.m2/settings.xml && \
    echo "    <server>" >> ~/.m2/settings.xml && \
    echo "      <id>github</id>" >> ~/.m2/settings.xml && \
    echo "      <username>technolog2007</username>" >> ~/.m2/settings.xml && \
    echo "      <password>${GITHUB_TOKEN}</password>" >> ~/.m2/settings.xml && \
    echo "    </server>" >> ~/.m2/settings.xml && \
    echo "  </servers>" >> ~/.m2/settings.xml && \
    echo "  <profiles>" >> ~/.m2/settings.xml && \
    echo "    <profile>" >> ~/.m2/settings.xml && \
    echo "      <id>github</id>" >> ~/.m2/settings.xml && \
    echo "      <repositories>" >> ~/.m2/settings.xml && \
    echo "        <repository>" >> ~/.m2/settings.xml && \
    echo "          <id>github</id>" >> ~/.m2/settings.xml && \
    echo "          <url>https://maven.pkg.github.com/technolog2007/telegram-discord-bot</url>" >> ~/.m2/settings.xml && \
    echo "          <releases><enabled>true</enabled></releases>" >> ~/.m2/settings.xml && \
    echo "          <snapshots><enabled>true</enabled></snapshots>" >> ~/.m2/settings.xml && \
    echo "        </repository>" >> ~/.m2/settings.xml && \
    echo "      </repositories>" >> ~/.m2/settings.xml && \
    echo "    </profile>" >> ~/.m2/settings.xml && \
    echo "  </profiles>" >> ~/.m2/settings.xml && \
    echo "  <activeProfiles>" >> ~/.m2/settings.xml && \
    echo "    <activeProfile>github</activeProfile>" >> ~/.m2/settings.xml && \
    echo "  </activeProfiles>" >> ~/.m2/settings.xml && \
    echo "</settings>" >> ~/.m2/settings.xml

# Виконуємо збірку проєкту за допомогою Maven
# Пропускає тести -DskipTests
RUN mvn clean package -DskipTests

# --- Етап виконання (Runtime stage) ---
FROM openjdk:17-jdk-slim

# Встановлення zip-архіватора та очищення кешу пакетів
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

# Створюємо робочу директорію всередині контейнера
WORKDIR /app

# Копіюємо зібраний ZIP-архів з попереднього етапу збірки
COPY --from=build /app/target/*.zip ./

# Розпаковка zip-архіву та видалення самого архіву
RUN unzip *.zip -d /app/ && rm *.zip

# Порт для обману Render (якщо це для Render.com)
EXPOSE 8080

# Запуск додатку
CMD java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar