package pkpm.telegrambot.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import pkpm.telegrambot.services.DiscordListener;
import pkpm.telegrambot.services.PkpmTelegramBot;

public class App {

  private static final Logger log = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) throws Exception {
    startTelegramBot();
    startDiscordListener();
  }

  private static void startTelegramBot() throws TelegramApiException {
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
    try {
      log.info("Registering bot...");
      telegramBotsApi.registerBot(new PkpmTelegramBot());
    } catch (TelegramApiRequestException e) {
      log.error(
          "Failed to register bot(check internet connection / bot token or make sure only one instance of bot is running).",
          e);
    }
    log.info("Telegram bot is ready to accept updates from user......");
  }

  private static void startDiscordListener() throws Exception {
    DiscordListener client = new DiscordListener();
    client.connectBlocking();
  }
}