package pkpm.telegrambot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import pkpm.telegrambot.models.ChatMessage;

@Slf4j
public class DiscordNotifier {

  private final String webhookUrl;
  private final int expectedCode = 204;

  private final String botToken = System.getenv("bot_token_discord");
  private final String channelId = System.getenv("chanel_id_discord");

  public DiscordNotifier(String webhookUrl) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      throw new IllegalArgumentException("Webhook URL cannot be null or empty.");
    }
    this.webhookUrl = webhookUrl.trim();
  }

  public void sendMessage(String message) {
    try {
      // Створюємо URL-об'єкт
      log.info("webhookURL is : {}", webhookUrl);
      log.info("botToken is : {}", botToken);
      log.info("channelId is : {}", channelId);
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

      checkResponseCode(responseCode, expectedCode, jsonPayload);

      connection.disconnect();
    } catch (Exception e) {
      log.error("An error occurred while sending a message to Discord: ", e);
    }
  }

  private void checkResponseCode(int responseCode, int expectedCode, String jsonPayload) {
    if (responseCode == expectedCode) {
      log.info(ChatMessage.DISCORD_RESPONSE_COD_204.getMessage());
    } else {
      log.error("Error sending: {}. Message: {}", responseCode, jsonPayload);
    }
  }
}