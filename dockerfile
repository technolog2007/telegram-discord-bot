# Базовий образ з JDK 17
FROM openjdk:17-jdk-slim

# Створюємо робочу директорію всередині контейнера
WORKDIR /app

# Копіюємо зібраний zip-архів до контейнера
COPY target/telegrambot-1.0.zip /app/

# Розпаковуємо архів
RUN apt-get update && apt-get install -y unzip && \
    unzip telegrambot-1.0.zip && \
    rm telegrambot-1.0.zip

# Перевірка структури (для відладки)
RUN ls -la /app

CMD echo "$APP_PROPERTIES" > config/app.properties && \
    java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar

# Виставляємо команду для запуску бота
CMD ["java", "-cp", "lib/*:config/*:.", "-jar", "telegrambot-1.0.jar"]
