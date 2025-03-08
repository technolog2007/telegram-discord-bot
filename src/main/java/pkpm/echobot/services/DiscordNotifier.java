package pkpm.echobot.services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscordNotifier {

  private final String webhookUrl;

  public DiscordNotifier(String webhookUrl) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      throw new IllegalArgumentException("Webhook URL cannot be null or empty.");
    }
    this.webhookUrl = webhookUrl.trim();
  }

  public void sendMessage(String message) {
    try {
      // Створюємо URL-об'єкт
      URL url = new URL(webhookUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("User-Agent", "Java Discord Bot");

      // Правильне форматування JSON, щоб уникнути помилок
      String safeMessage = message.replace("\"", "\\\"");
      String jsonPayload = String.format("{\"content\": \"%s\"}", safeMessage);
      byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

      // Відправка даних
      try (OutputStream os = connection.getOutputStream()) {
        os.write(payloadBytes);
        os.flush();
      }

      // Перевірка відповіді
      int responseCode = connection.getResponseCode();
      log.info("Discord response code: {}", responseCode);

      if (responseCode == 204) {
        log.info("Повідомлення успішно надіслано в Discord!");
      } else {
        log.error("Помилка при надсиланні: {}. Повідомлення: {}", responseCode, jsonPayload);
      }

      connection.disconnect();
    } catch (Exception e) {
      log.error("Виникла помилка при відправленні повідомлення в Discord: ", e);
    }
  }
}