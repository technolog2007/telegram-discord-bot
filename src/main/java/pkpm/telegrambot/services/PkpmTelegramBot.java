package pkpm.telegrambot.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private static final String START_TEXT = "⬇️ Натисніть відповідну кнопку:";
  private final Map<Long, String> input = new HashMap<>();
  private String firstPress = null;
  private String secondPress = null;
  private static final String BUTTON_1 = "\uD83D\uDCC1 Нова вкладка";
  private static final String BUTTON_2 = "\uD83D\uDCCB Нові позиції";
  private static final String BUTTON_3 = "\uD83D\uDD04 Зміни";
  private static final String BUTTON_4 = "✅ Підтвердити";
  private static final String BUTTON_5 = "❌ Скасувати";
  private static final String ACCEPT_MESSAGE = "Повідомлення відправлено!\uD83D\uDE80";
  private static final String NEGATIVE_MESSAGE = "Повідомлення не відправлено!\uD83D\uDEAB";
  private String sendMessage = "";

  private DiscordNotifier notifier = new DiscordNotifier(System.getenv("web_hook_discord"));

  @Override
  public String getBotUsername() {
    return System.getenv("bot_user_name");
  }

  @Override
  public String getBotToken() {
    return System.getenv("token");
  }

  @Override
  public void onUpdateReceived(Update update) {
    String userName = update.getMessage().getFrom().getFirstName();
    String message = update.getMessage().getText();
    Long chatId = update.getMessage().getChatId();
    boolean flagUserChat = update.getMessage().getChat().isUserChat();
    boolean flagHasText = update.getMessage().hasText();
    Long groupId = Long.parseLong(System.getenv("group_test_id"));
    log.info("Update 1 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    if (flagUserChat && flagHasText && firstPress == null) {
      sendMenu(chatId);
      input.put(chatId, message);
    }
    log.info("Update 2 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    firstPress = checkWhichButtonFirstIsPress(update, flagUserChat);
    secondPress = checkWhichButtonSecondIsPress(update);
    firstPress(chatId, userName, flagUserChat, flagHasText, message);
    log.info("Update 3 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    secondPress(flagUserChat, flagHasText, groupId, chatId);
    log.info("Update 4 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
  }

  private void firstPress(Long chatId, String userName, boolean flagUserChat, boolean flagHasText,
      String message) {
    if (flagUserChat && flagHasText && firstPress != null && secondPress == null) {
      if (firstPress.equals(BUTTON_1) && !message.equals(BUTTON_1)) {
        sendMessage(createMessage(chatId, "Буде надіслано наступне повідомлення :"));
        sendMessage = "Додана нова вкладка : \"" + message + "\"";
        sendReplyButtons(chatId, sendMessage);
      }
      if (firstPress.equals(BUTTON_2) && !message.equals(BUTTON_2)) {
        sendMessage(createMessage(chatId, "Буде надіслано наступне повідомлення :"));
        sendMessage = "Додані нові позиції на вкладку : \"" + message + "\"";
        sendReplyButtons(chatId, sendMessage);
      }
      if (firstPress.equals(BUTTON_3)) {
        sendReplyButtons(chatId, "Ви впевнені, що хочете внести зміни?");
      }
    }
  }

  private void secondPress(boolean flagUserChat, boolean flagHasText, Long groupId, Long chatId) {
    if (flagUserChat && flagHasText && firstPress != null && secondPress != null) {
      if (secondPress.equals("Підтвердити створення нової вкладки")) {
        sendMessage(createMessage(groupId, sendMessage));
        notifier.sendMessage(sendMessage);
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      }
      if (secondPress.equals("Скасувати створення нової вкладки")) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      if (secondPress.equals("Підтвердити додавання нових позицій")) {
        sendMessage(createMessage(groupId, sendMessage));
        notifier.sendMessage(sendMessage);
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      }
      if (secondPress.equals("Скасувати додавання нових позицій")) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      if (secondPress.equals("Підтвердити зміни")) {
        sendMessage(createMessage(groupId, "Додані нові позиції на вкладку \"Зміни\""));
        notifier.sendMessage("Додані нові позиції на вкладку \"Зміни\"");
        sendMessage(createMessage(chatId, ACCEPT_MESSAGE));
      }
      if (secondPress.equals("Скасувати зміни")) {
        sendMessage(createMessage(chatId, NEGATIVE_MESSAGE));
      }
      firstPress = null;
      secondPress = null;
      sendMenu(chatId);
    }
  }

  private String checkWhichButtonSecondIsPress(Update update) {
    String messageText = update.getMessage().getText();
    if (firstPress != null && secondPress == null) {
      if (firstPress.equals(BUTTON_1) && messageText.equals(BUTTON_4)) {
        return "Підтвердити створення нової вкладки";
      } else if ((firstPress.equals(BUTTON_1) && messageText.equals(BUTTON_5))) {
        return "Скасувати створення нової вкладки";
      }
      if (firstPress.equals(BUTTON_2) && messageText.equals(BUTTON_4)) {
        return "Підтвердити додавання нових позицій";
      } else if ((firstPress.equals(BUTTON_2) && messageText.equals(BUTTON_5))) {
        return "Скасувати додавання нових позицій";
      }
      if (firstPress.equals(BUTTON_3) && messageText.equals(BUTTON_4)) {
        return "Підтвердити зміни";
      } else if ((firstPress.equals(BUTTON_3) && messageText.equals(BUTTON_5))) {
        return "Скасувати зміни";
      }
    }
    return null;
  }

  private String checkWhichButtonFirstIsPress(Update update, boolean flagUserChat) {
    String messageText = update.getMessage().getText();
    Long chatId = update.getMessage().getChatId();
    if (flagUserChat && BUTTON_1.equals(messageText)) {
      sendMessage(createMessage(chatId, "Введіть позначення вкладки:"));
      return messageText;
    } else if (flagUserChat && BUTTON_2.equals(messageText)) {
      sendMessage(createMessage(chatId, "Введіть позначення вкладки на яку додані позиції:"));
      return messageText;
    } else if (flagUserChat && BUTTON_3.equals(messageText)) {
      return messageText;
    } else if (firstPress != null) {
      return firstPress;
    }
    return null;
  }

  private void sendReplyButtons(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    message.setReplyMarkup(createConfirmationKeyboard()); // Додаємо клавіатуру
//    message.setReplyMarkup(InlineKeyboardBuilder.createSingleRowKeyBoard(button4, button5));
    try {
      execute(message); // Відправляємо повідомлення
    } catch (Exception e) {
      e.printStackTrace();
    }
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

  private ReplyKeyboardMarkup createConfirmationKeyboard() {
    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    keyboardMarkup.setResizeKeyboard(true);

    KeyboardRow row = new KeyboardRow();
    row.add(BUTTON_4);
    row.add(BUTTON_5);

    List<KeyboardRow> keyboard = new ArrayList<>();
    keyboard.add(row);

    keyboardMarkup.setKeyboard(keyboard);
    return keyboardMarkup;
  }
}