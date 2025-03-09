# Базовий образ для JDK 17 (або інший, залежно від твого проєкту)
FROM eclipse-temurin:17-jdk
# Встановлення робочої директорії
WORKDIR /app
# Копіювання jar-файлу в контейнер
COPY target/*.jar echobot-1.0.jar
# Запуск програми
CMD ["java", "-jar", "echobot-1.0.jar"]