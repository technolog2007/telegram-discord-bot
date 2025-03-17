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
import java.nio.charset.StandardCharsets;

@Slf4j
public class DiscordListener extends WebSocketClient {

  private static final String DISCORD_GATEWAY = System.getenv("discord_gateway");
  private final String botToken = System.getenv("bot_token_discord");
  private final ObjectMapper objectMapper = new ObjectMapper();

  private Session session;

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
        log.info("New message from {}: {}", author, content);
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
    log.warn("Reconnecting to Discord...");
    disconnect(); // Закриваємо поточне з'єднання
    try {
      Thread.sleep(5000); // Затримка перед новим підключенням (5 сек)
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Reconnect sleep interrupted", e);
    }
    connect(); // Встановлюємо нове з'єднання
  }

  public void connect() {
    try {
      URI discordGateway = new URI(System.getenv("discord_gateway"));
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      session = container.connectToServer(this, discordGateway);

      log.info("Connected to Discord Gateway!");
    } catch (Exception e) {
      log.error("Failed to connect to Discord Gateway", e);
      scheduleReconnect(); // Якщо підключення не вдалося, пробуємо ще раз
    }
  }

  private void disconnect() {
    if (session != null && session.isOpen()) {
      try {
        session.close();
        log.info("Disconnected from Discord Gateway");
      } catch (IOException e) {
        log.error("Error closing WebSocket connection", e);
      }
    }
  }

  private void scheduleReconnect() {
    new Thread(() -> {
      try {
        Thread.sleep(10000); // Чекаємо 10 секунд перед повторним підключенням
        reconnect();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Reconnect scheduling interrupted", e);
      }
    }).start();
  }

//  public static void main(String[] args) throws Exception {
//    DiscordListener client = new DiscordListener();
//    client.connectBlocking();
//  }
}

