package pkpm.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import pkpm.company.automation.services.GraphExecutionReport;
import pkpm.telegrambot.services.discord.DiscordCorrectionBot;
import pkpm.telegrambot.services.discord.DiscordListener;
import pkpm.telegrambot.services.discord.DiscordNotifier;
import pkpm.telegrambot.services.service.PkpmTelegramBot;
import pkpm.telegrambot.services.service.ReportService;
import pkpm.telegrambot.services.service.ReportServiceImpl;
import pkpm.telegrambot.utils.FileScanner;

public class App {

  private static final Logger log = LoggerFactory.getLogger(App.class);
  private static final String REPORT_FILE_NAME = System.getenv("REPORT_FILE_NAME");
  private static final String GRAPH_NAME = System.getenv("GRAPH_NAME");
  private static final String GROUP_VTVS_ID = System.getenv("GROUP_TEST_ID");
  private static final String BOT_USER_NAME = System.getenv("BOT_USER_NAME");
  private static final String BOT_TOKEN_TELEGRAM = System.getenv("BOT_TOKEN_TELEGRAM");
  private static final String USERS_LIST = System.getenv("USERS_LIST");
  private static final String WEB_HOOK_DISCORD = System.getenv("WEB_HOOK_DISCORD");
  private static final long GROUP_TEST_ID = Long.parseLong(System.getenv("GROUP_TEST_ID"));

  public static void main(String[] args) throws Exception {
    DiscordNotifier discordNotifier = new DiscordNotifier(WEB_HOOK_DISCORD);
    ReportService reportService = new ReportServiceImpl(new GraphExecutionReport());
    PkpmTelegramBot bot = new PkpmTelegramBot(BOT_USER_NAME, BOT_TOKEN_TELEGRAM, GROUP_TEST_ID,
        USERS_LIST, GRAPH_NAME, discordNotifier, reportService);
    startTelegramBot(bot);
//    startDiscordListener();
    DiscordCorrectionBot.main(args);
    FileScanner scanner = new FileScanner(REPORT_FILE_NAME);
    log.info("File name is {}", REPORT_FILE_NAME);
    while (true) {
      log.info("file scanner working");
      scanner.scanner();
      bot.sendMessageAndCleanFile(toLongFromString(GROUP_VTVS_ID), REPORT_FILE_NAME);
    }
  }

  private static void startTelegramBot(PkpmTelegramBot bot) throws TelegramApiException {
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
    try {
      log.info("Registering bot...");
      telegramBotsApi.registerBot(bot);
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

  private static Long toLongFromString(String line) {
    return Long.valueOf(line);
  }
}