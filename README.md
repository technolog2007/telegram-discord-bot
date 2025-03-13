# telegram-discord-bot

## Запуск

1.  Розархівуйте ZIP-архів.
2.  Перейдіть до каталогу, де знаходиться JAR-файл.
3.  Запустіть проєкт за допомогою команди:
4.  Додайте from.env в корінь проекту

    ```bash
    for /f "delims=" %%a in (from.env) do set "%%a"
    ```
    ```bash
    java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar
    ```