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

# Копіюємо конфігураційний файл з секретами з Render
# Секрети в Render зазвичай доступні за шляхом /secrets
COPY /secrets/APP_PROPERTIES /app/config/app.properties

# Переконайтесь, що файл скопійовано правильно
RUN cat /app/config/app.properties

# Команда для запуску додатку
CMD java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar
