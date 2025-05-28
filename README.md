# telegram-discord-bot

## Запуск для windows

1.  Розархівуйте ZIP-архів.
2.  Перейдіть до каталогу, де знаходиться JAR-файл.
3.  Додайте **`from.env`** у кореневий каталог, де знаходиться JAR-файл. Цей файл повинен містити 
необхідні змінні середовища: BOT_TOKEN_TELEGRAM, BOT_USER_NAME, GROUP_TEST_ID, GROUP_VTVS_ID, 
USERS_LIST, WEB_HOOK_DISCORD, DISCORD_GATEWAY, BOT_TOKEN_DISCORD, CHANEL_ID_DISCORD, TEMPORARY_PATH, 
INTERVAL_TIME, FILE_NAME, FILE_REPORT_GENERAL, GRAPH_NAME
4.  Створіть **`run_bot.bat`** в корні, де знаходиться JAR-файл. Цей файл повинен
містити наступний скрипт:

```batch
@echo off
setlocal

set ENV_FILE=from.env

if not exist %ENV_FILE% (
echo Error: .env file not found at %ENV_FILE%
exit /b 1
)

for /f "tokens=1* delims==" %%a in (%ENV_FILE%) do (
rem Пропускаємо порожні рядки та коментарі, що починаються з #
if not "%%a"=="" (
if not "%%a"=="rem" (
if not "%%a"=="::" (
if not "%%a:~0,1"=="#" (
set "%%a=%%b"
)
)
)
)
)

rem Запускаємо JAR-файл
rem Переконайтеся, що lib\ та config\ знаходяться відносно місця запуску скрипта
java -cp "lib/*;config/*;." -jar telegrambot-1.0.jar

endlocal 
```

5. Запустіть bat-файл

## Запуск для Linux/macOS

1.  Розархівуйте ZIP-архів.
2.  Перейдіть до каталогу, де знаходиться JAR-файл.
3.  Додайте **`from.env`** у кореневий каталог, де знаходиться JAR-файл. Цей файл повинен містити 
    необхідні змінні середовища: BOT_TOKEN_TELEGRAM, BOT_USER_NAME, GROUP_TEST_ID, GROUP_VTVS_ID, 
    USERS_LIST, WEB_HOOK_DISCORD, DISCORD_GATEWAY, BOT_TOKEN_DISCORD, CHANEL_ID_DISCORD,
    TEMPORARY_PATH, INTERVAL_TIME, FILE_NAME, FILE_REPORT_GENERAL, GRAPH_NAME
4.  Створіть **`run_bot.sh`** в корні, де знаходиться JAR-файл. Цей файл повинен
    містити наступний скрипт:

```batch
#!/bin/bash

# Шлях до вашого файлу .env
ENV_FILE="from.env"

# Перевіряємо, чи існує файл .env
if [ ! -f "$ENV_FILE" ]; then
echo "Error: .env file not found at $ENV_FILE"
exit 1
fi

# Завантажуємо змінні середовища з файлу .env
# Цей цикл зчитує кожний рядок, пропускає коментарі та порожні рядки,
# та експортує змінну.
while IFS='=' read -r key value; do
if [[ -n "$key" && ! "$key" =~ ^# ]]; then # Перевірка, що ключ не порожній і не коментар
export "$key"="$value"
fi
done < "$ENV_FILE"

# Запускаємо JAR-файл
# Переконайтеся, що lib/ та config/ знаходяться відносно місця запуску скрипта
java -cp "lib/*:config/*:." -jar telegrambot-1.0.jar
```

5. Зробіть скрипт виконуваним: chmod +x run_bot.sh
6. Запустіть скрипт: ./run_bot.sh
