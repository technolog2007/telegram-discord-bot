package pkpm.telegrambot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

@Slf4j
public class DiscordListener extends WebSocketClient {

  private static final String DISCORD_GATEWAY = System.getenv("discord_gateway");
  private final String botToken = System.getenv("bot_token_discord");
  private final ObjectMapper objectMapper = new ObjectMapper();

  private Session session;
  private static final int MAX_RECONNECT_ATTEMPTS = 5;
  private int reconnectAttempts = 0;

  public DiscordListener() throws Exception {
    super(new URI(DISCORD_GATEWAY));
  }

  @Override
  public void onOpen(ServerHandshake handshakedata) {
    log.info("Connected to Discord Gateway!");
  }

  private void sendHeartbeat() {
    String heartbeatPayload = "{\"op\": 1, \"d\": null}";
    send(heartbeatPayload);
    log.info("Sent Heartbeat");
  }

  @Override
  public void onMessage(String message) {
    try {
      JsonNode jsonNode = objectMapper.readTree(message);
      String opCode = jsonNode.get("op").asText();
      if ("10".equals(opCode)) {  // Hello packet, start heartbeat
        int heartbeatInterval = jsonNode.get("d").get("heartbeat_interval").asInt();
        sendIdentify();
        log.info("Received Hello. Heartbeat interval: {}", heartbeatInterval);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 41250, TimeUnit.MILLISECONDS);
      } else if ("0".equals(opCode) && "MESSAGE_CREATE".equals(jsonNode.get("t").asText())) {
        JsonNode data = jsonNode.get("d");
        String content = data.get("content").asText();
        String author = data.get("author").get("username").asText();
//        log.info("Raw Discord Message JSON: {}", data.toPrettyString());
        log.info("New message from {}: {}", author, content);
//        if (data.has("type")) {
//          int messageType = data.get("type").asInt();
//          log.info("📝 Message type: {}", messageType);
//        }
      } else if ("11".equals(opCode)) {
        log.info("Received Heartbeat ACK");
      }
    } catch (Exception e) {
      log.error("Error processing message: ", e);
    }
  }

  private void sendIdentify() {
    String payload = String.format(
        "{\"op\": 2, \"d\": { \"token\": \"%s\", \"intents\": 513, \"properties\": {\"$os\": \"linux\", \"$browser\": \"java\", \"$device\": \"java\"}}}",
        botToken);
    send(payload);
    log.info("Sent IDENTIFY packet");
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    log.warn("Disconnected from Discord: {} - {}", code, reason);
  }

  @Override
  public void onError(Exception ex) {
    log.error("WebSocket error: ", ex);
  }

  public void reconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      log.error("Max reconnect attempts reached. Stopping reconnection.");
      return;
    }

    reconnectAttempts++;
    log.warn("Reconnecting to Discord... Attempt {}/{}", reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

    disconnect();
    try {
      Thread.sleep(5000 * reconnectAttempts);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Reconnect sleep interrupted", e);
      return;
    }

    connect();
  }

  public void connect() {
    try {
      super.connect(); // Викликаємо оригінальний метод WebSocketClient
      log.info("Connecting to Discord Gateway...");
    } catch (Exception e) {
      log.error("Failed to connect to Discord Gateway", e);
      scheduleReconnect();
    }
  }

  private void disconnect() {
    try {
      if (this.isOpen()) {
        this.closeBlocking();
        log.info("Disconnected from Discord Gateway");
      }
    } catch (InterruptedException e) {
      log.error("Error closing WebSocket connection", e);
      Thread.currentThread().interrupt();
    }
  }

  private void scheduleReconnect() {
    new Thread(() -> {
      try {
        Thread.sleep(10000);
        reconnect();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Reconnect scheduling interrupted", e);
      }
    }).start();
  }
}

