package pkpm.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import pkpm.telegrambot.services.discord.DiscordListener;
import pkpm.telegrambot.services.telegram.PkpmTelegramBot;
import pkpm.telegrambot.utils.FileScanner;

public class App {

  private static final Logger log = LoggerFactory.getLogger(App.class);
  private static final String FILE_NAME = System.getenv("FILE_NAME");
  private static final String GROUP_VTVS_ID = System.getenv("GROUP_TEST_ID");

  public static void main(String[] args) throws Exception {
    startTelegramBot();
//    startDiscordListener();
    FileScanner scanner = new FileScanner(FILE_NAME);
    log.info("File name is {}", FILE_NAME);
    while (true) {
      log.info("file scanner working");
      scanner.scanner();
      PkpmTelegramBot.getInstance().sendMessageAndCleanFile(toLongFromString(GROUP_VTVS_ID), FILE_NAME);
    }
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

  private static Long toLongFromString(String line){
    return Long.valueOf(line);
  }
}