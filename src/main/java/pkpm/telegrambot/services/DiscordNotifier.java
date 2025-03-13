package pkpm.telegrambot.services;

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

      // Proper JSON formatting to avoid errors
      String safeMessage = message.replace("\"", "\\\"");
      String jsonPayload = String.format("{\"content\": \"%s\"}", safeMessage);
      byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

      // Sending data
      try (OutputStream os = connection.getOutputStream()) {
        os.write(payloadBytes);
        os.flush();
      }

      // Answer check
      int responseCode = connection.getResponseCode();
      log.info("Discord response code: {}", responseCode);

      if (responseCode == 204) {
        log.info("Message successfully sent to Discord!");
      } else {
        log.error("Error sending: {}. Message: {}", responseCode, jsonPayload);
      }

      connection.disconnect();
    } catch (Exception e) {
      log.error("An error occurred while sending a message to Discord: ", e);
    }
  }
}