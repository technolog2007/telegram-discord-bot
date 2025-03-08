package pkpm.echobot.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import pkpm.echobot.util.PropertiesLoader;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private final Map<Long, String> input = new HashMap<>();
  private String firstPress = null;
  private String secondPress = null;
  private final String button1 = "\uD83D\uDCC1 Нова вкладка";
  private final String button2 = "\uD83D\uDCCB Нові позиції";
  private final String button3 = "\uD83D\uDD04 Зміни";
  private final String button4 = "✅ Підтвердити";
  private final String button5 = "❌ Скасувати";
  private final String acceptMessage = "Повідомлення відправлено!\uD83D\uDE80";
  private final String negativeMessage = "Повідомлення не відправлено!\uD83D\uDEAB";
  private String sendMessage = "";

  private DiscordNotifier notifier = new DiscordNotifier(
      new PropertiesLoader().loadProperties().getProperty("web_hook_discord"));

  @Override
  public String getBotUsername() {
    return new PropertiesLoader().loadProperties().getProperty("bot_user_name");
  }

  @Override
  public String getBotToken() {
    return new PropertiesLoader().loadProperties().getProperty("token");
  }

  @Override
  public void onUpdateReceived(Update update) {
    String userName = update.getMessage().getFrom().getFirstName();
    String message = update.getMessage().getText();
    Long chatId = update.getMessage().getChatId();
    boolean flagUserChat = update.getMessage().getChat().isUserChat();
    boolean flagHasText = update.getMessage().hasText();
    Long groupId = Long.parseLong(
        new PropertiesLoader().loadProperties().getProperty("group_test_id"));
    log.info("Update 1 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    firstPress = checkWhichButtonFirstIsPress(update, flagUserChat);
    secondPress = checkWhichButtonSecondIsPress(update);
    if (flagUserChat && flagHasText && firstPress == null) {
      sendMenu(chatId);
      input.put(chatId, message);
    }
    log.info("Update 2 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    if (flagUserChat && flagHasText && firstPress != null && secondPress == null) {
      if (firstPress.equals(button1) && !message.equals(button1)) {
        sendText(chatId, "Буде надіслано наступне повідомлення :");
        sendMessage = "Додана нова вкладка : \"" + message + "\"";
        sendReplyButtons(chatId, sendMessage);
      }
      if (firstPress.equals(button2) && !message.equals(button2)) {
        sendText(chatId, "Буде надіслано наступне повідомлення :");
        sendMessage = "Додані нові позиції на вкладку : \"" + message + "\"";
        sendReplyButtons(chatId, sendMessage);
      }
      if (firstPress.equals(button3)) {
        sendReplyButtons(chatId, "Ви впевнені, що хочете внести зміни?");
      }
    }
    log.info("Update 3 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
    if (flagUserChat && flagHasText && firstPress != null && secondPress != null) {
      if (secondPress.equals("Підтвердити створення нової вкладки")) {
        sendText(groupId, sendMessage);
        notifier.sendMessage(sendMessage);
        sendText(chatId, acceptMessage);
      }
      if (secondPress.equals("Скасувати створення нової вкладки")) {
        sendText(chatId, negativeMessage);
      }
      if (secondPress.equals("Підтвердити додавання нових позицій")) {
        sendText(groupId, sendMessage);
        notifier.sendMessage(sendMessage);
        sendText(chatId, acceptMessage);
      }
      if (secondPress.equals("Скасувати додавання нових позицій")) {
        sendText(chatId, negativeMessage);
      }
      if (secondPress.equals("Підтвердити зміни")) {
        sendText(groupId, "Додані нові позиції на вкладку \"Зміни\"");
        notifier.sendMessage("Додані нові позиції на вкладку \"Зміни\"");
        sendText(chatId, acceptMessage);
      }
      if (secondPress.equals("Скасувати зміни")) {
        sendText(chatId, negativeMessage);
      }
      firstPress = null;
      secondPress = null;
      sendMenu(chatId);
    }
    log.info("Update 4 : {}, {}, \"{}\", {}, {}", userName, chatId, message, firstPress,
        secondPress);
  }

  private String checkWhichButtonSecondIsPress(Update update) {
    String messageText = update.getMessage().getText();
    if (firstPress != null && secondPress == null) {
      if (firstPress.equals(button1) && messageText.equals(button4)) {
        return "Підтвердити створення нової вкладки";
      } else if ((firstPress.equals(button1) && messageText.equals(button5))) {
        return "Скасувати створення нової вкладки";
      }
      if (firstPress.equals(button2) && messageText.equals(button4)) {
        return "Підтвердити додавання нових позицій";
      } else if ((firstPress.equals(button2) && messageText.equals(button5))) {
        return "Скасувати додавання нових позицій";
      }
      if (firstPress.equals(button3) && messageText.equals(button4)) {
        return "Підтвердити зміни";
      } else if ((firstPress.equals(button3) && messageText.equals(button5))) {
        return "Скасувати зміни";
      }
    }

    return null;
  }

  private String checkWhichButtonFirstIsPress(Update update, boolean flagUserChat) {
    String messageText = update.getMessage().getText();
    if (flagUserChat && button1.equals(messageText)) {
      sendText(update.getMessage().getChatId(), "Введіть позначення вкладки:");
      return messageText;
    } else if (flagUserChat && button2.equals(messageText)) {
      sendText(update.getMessage().getChatId(),
          "Введіть позначення вкладки на яку додані позиції:");
      return messageText;
    } else if (flagUserChat && button3.equals(messageText)) {
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
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("⬇\uFE0F Натисніть відповідну кнопку:");

    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    keyboardMarkup.setResizeKeyboard(true);
    keyboardMarkup.setOneTimeKeyboard(false);
    keyboardMarkup.setSelective(true);

    List<KeyboardRow> keyboard = new ArrayList<>();
    KeyboardRow rowHigh = new KeyboardRow();
    rowHigh.add(button1); // кнопка для введення тексту
    rowHigh.add(button2);

    KeyboardRow rowLow = new KeyboardRow();
    rowLow.add(button3);

    keyboard.add(rowHigh);
    keyboard.add(rowLow);

    keyboardMarkup.setKeyboard(keyboard);
    message.setReplyMarkup(keyboardMarkup);

    try {
      execute(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Метод створює і надсилає у відповідний чат повідомлення
   *
   * @param chatId - id чата
   * @param text   - повідомлення
   */
  private void sendText(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
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
    row.add(button4);
    row.add(button5);

    List<KeyboardRow> keyboard = new ArrayList<>();
    keyboard.add(row);

    keyboardMarkup.setKeyboard(keyboard);
    return keyboardMarkup;
  }
}
