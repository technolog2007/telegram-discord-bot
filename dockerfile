# Базовий образ з JDK 17
FROM openjdk:17-jdk-slim

# Встановлення Maven
RUN apt-get update && apt-get install -y maven

# Встановлення zip-архіватора
RUN apt-get update && apt-get install -y unzip

# Створюємо робочу директорію всередині контейнера
WORKDIR /app

# Копіюємо всі файли проєкту (включаючи pom.xml та вихідний код)
COPY . .

# Виконуємо збірку проєкту за допомогою Maven
RUN mvn clean package

# Розпаковка zip-архіву, створеного Maven
RUN unzip target/*.zip -d /app/

# # Додаємо змінну середовища для конфігів
# ENV APP_PROPERTIES=""

# Команда запуску JAR-файлу
CMD echo "$APP_PROPERTIES" > config/app.properties && \
    java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar
