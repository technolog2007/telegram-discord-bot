package pkpm.telegrambot.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private static final String START_TEXT = "⬇️ Натисніть відповідну кнопку:";
  private final Map<Long, String> input = new HashMap<>();
  private String menuButtonPres = null;
  private String replyButtonPress = null;
  private static final String BUTTON_1 = "\uD83D\uDCC1 Нова вкладка";
  private static final String BUTTON_2 = "\uD83D\uDCCB Нові позиції";
  private static final String BUTTON_3 = "\uD83D\uDD04 Зміни";
  private static final String BUTTON_4 = "✅ Підтвердити";
  private static final String BUTTON_5 = "❌ Скасувати";
  private static final String ACCEPT_MESSAGE = "Повідомлення відправлено!\uD83D\uDE80";
  private static final String NEGATIVE_MESSAGE = "Повідомлення не відправлено!\uD83D\uDEAB";
  private String compositeMessage = "";

  private DiscordNotifier notifier = new DiscordNotifier(System.getenv("web_hook_discord"));

  @Override
  public String getBotUsername() {
    return System.getenv("bot_user_name");
  }

  @Override
  public String getBotToken() {
    return System.getenv("token");
  }

  private Long getGroupId() {
    return Long.parseLong(System.getenv("group_test_id"));
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasCallbackQuery()) {
      replyButtonPress = update.getCallbackQuery().getData();
      Long chatId = update.getCallbackQuery().getMessage().getChatId();
//      Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
      replyButtonReaction(getGroupId(), chatId); // логіка в залежності від того яка кнопка меню і яка кнопка підтвердження була натиснута
      log.info("Update query : {}, {}, {}, {}", chatId, getGroupId(), menuButtonPres,
          replyButtonPress);
    } else if (update.getMessage().getChat().isUserChat() && update.getMessage().hasText()) {
      String userName = update.getMessage().getFrom().getFirstName();
      String message = update.getMessage().getText();
      Long chatId = update.getMessage().getChatId();
      boolean flagUserChat = update.getMessage().getChat().isUserChat();
      boolean flagHasText = update.getMessage().hasText();
      log.info("Update 1 : {}, {}, \"{}\", {}", userName, chatId, message, menuButtonPres);
      menuButtonPres = checkWhichButtonFirstIsPress(chatId, message,
          flagUserChat); // визначенн яка кнопка меню була натиснута
      log.info("Update 2 : {}, {}, \"{}\", {}", userName, chatId, message, menuButtonPres);
      createMenu(flagUserChat, flagHasText, chatId, message); // створення меню
      log.info("Update 3 : {}, {}, \"{}\", {}", userName, chatId, message, menuButtonPres);
      menuPressButtonReaction(chatId, userName, flagUserChat, flagHasText,
          message); // логіка, в залежності від того яка кнопка меню була натиснута
      log.info("Update 4 : {}, {}, \"{}\", {}", userName, chatId, message, menuButtonPres);
    }
  }

  private void createMenu(boolean flagUserChat, boolean flagHasText, Long chatId, String message) {
    if (flagUserChat && flagHasText && menuButtonPres == null) {
      sendMenu(chatId);
      input.put(chatId, message);
    }
  }

  /**
   * Метод аналізує яка кнопка із меню була натиснута і виконує відповідну логіку
   *
   * @param chatId
   * @param userName
   * @param flagUserChat
   * @param flagHasText
   * @param message
   */
  private void menuPressButtonReaction(Long chatId, String userName, boolean flagUserChat,
      boolean flagHasText,
      String message) {
    if (flagUserChat && flagHasText && menuButtonPres != null) {
      if (menuButtonPres.equals(BUTTON_1) && !message.equals(BUTTON_1)) {
        sendMessage(createMessage(chatId, "Буде надіслано наступне повідомлення :"));
        compositeMessage = "Додана нова вкладка : \"" + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButtonPres.equals(BUTTON_2) && !message.equals(BUTTON_2)) {
        sendMessage(createMessage(chatId, "Буде надіслано наступне повідомлення :"));
        compositeMessage = "Додані нові позиції на вкладку : \"" + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButtonPres.equals(BUTTON_3)) {
        sendReplyButtons(chatId, "Ви впевнені, що хочете внести зміни?");
      }
    }
  }

  private void replyButtonReaction(Long groupId, Long chatId) {
    if (menuButtonPres != null) {
      if (menuButtonPres.equals(BUTTON_1) && replyButtonPress.equals(BUTTON_4)) {
        sendMessage(createMessage(groupId, compositeMessage));
        notifier.sendMessage(compositeMessage);
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      } else if (menuButtonPres.equals(BUTTON_1) && replyButtonPress.equals(BUTTON_5)) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      if (menuButtonPres.equals(BUTTON_2) && replyButtonPress.equals(BUTTON_4)) {
        sendMessage(createMessage(groupId, compositeMessage));
        notifier.sendMessage(compositeMessage);
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      } else if (menuButtonPres.equals(BUTTON_2) && replyButtonPress.equals(BUTTON_5)) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      if (menuButtonPres.equals(BUTTON_3) && replyButtonPress.equals(BUTTON_4)) {
        sendMessage(createMessage(groupId, "Додані нові позиції на вкладку \"Зміни\""));
        notifier.sendMessage("Додані нові позиції на вкладку \"Зміни\"");
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      } else if (menuButtonPres.equals(BUTTON_3) && replyButtonPress.equals(BUTTON_5)) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      menuButtonPres = null;
//      replyButtonPress = null;
//      sendMenu(chatId);
    }
  }

  /**
   * Метод перевіряє яка кнопка із меню натиснута і надсилає повідомлення для продовження діалогу
   *
   * @param flagUserChat
   * @return
   */
  private String checkWhichButtonFirstIsPress(Long chatId, String messageText,
      boolean flagUserChat) {
    if (flagUserChat && BUTTON_1.equals(messageText)) {
      sendMessage(createMessage(chatId, "Введіть позначення вкладки:"));
      return messageText;
    } else if (flagUserChat && BUTTON_2.equals(messageText)) {
      sendMessage(createMessage(chatId, "Введіть позначення вкладки на яку додані позиції:"));
      return messageText;
    } else if (flagUserChat && BUTTON_3.equals(messageText)) {
      return messageText;
    } else if (menuButtonPres != null) {
      return menuButtonPres;
    }
    return null;
  }

  /**
   * Мтеод створює додаткову клавіатуру
   *
   * @param chatId
   * @param text
   */
  private void sendReplyButtons(Long chatId, String text) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(text).build();
    message.setReplyMarkup(InlineKeyboardBuilder.createSingleRowKeyboard(BUTTON_4, BUTTON_5));
    sendMessage(message);
  }

  /**
   * Метод створює меню з кнопок для відповідного чату
   *
   * @param chatId - id відповідного чату
   */
  private void sendMenu(Long chatId) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(START_TEXT).build();
    String[] firstRow = {BUTTON_1, BUTTON_2};
    String[] secondRow = {BUTTON_3};

    ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardBuilder.createMultiRowKeyboard(
        List.of(firstRow, secondRow));
    keyboardMarkup.setResizeKeyboard(true);
    keyboardMarkup.setOneTimeKeyboard(false);
    keyboardMarkup.setSelective(true);

    message.setReplyMarkup(keyboardMarkup);
    sendMessage(message);
  }

  /**
   * Метод створює і надсилає текстове повідомлення в чат
   *
   * @param chatId  - id чата
   * @param message - повідомлення
   */
  private SendMessage createMessage(Long chatId, String message) {
    return SendMessage.builder().chatId(chatId).text(message).build();
  }

  /**
   * Метод створює і надсилає у відповідний чат повідомлення
   *
   * @param message
   */
  private void sendMessage(SendMessage message) {
    try {
      execute(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

//  private ReplyKeyboardMarkup createConfirmationKeyboard() {
//    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//    keyboardMarkup.setResizeKeyboard(true);
//
//    KeyboardRow row = new KeyboardRow();
//    row.add(BUTTON_4);
//    row.add(BUTTON_5);
//
//    List<KeyboardRow> keyboard = new ArrayList<>();
//    keyboard.add(row);
//
//    keyboardMarkup.setKeyboard(keyboard);
//    return keyboardMarkup;
//  }
}