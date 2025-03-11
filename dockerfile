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

# Порт для обману Render
EXPOSE 8080

# Виконуємо збірку проєкту за допомогою Maven
RUN mvn clean package

# Розпаковка zip-архіву, створеного Maven
RUN unzip target/*.zip -d /app/

# Команда для запуску додатку
CMD java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar & \
while true; do echo -e "HTTP/1.1 200 OK\n\nBot is running" | nc -l -p 8080; done
