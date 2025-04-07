package pkpm.telegrambot.services.telegram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import pkpm.telegrambot.models.Buttons;
import pkpm.telegrambot.models.ChatMessage;
import pkpm.telegrambot.services.discord.DiscordNotifier;
import pkpm.telegrambot.utils.MessageReader;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private static PkpmTelegramBot instance;
  private final Map<Long, String> input = new HashMap<>();
  private String menuButton = null;
  private String replyButton = null;
  private String compositeMessage = "";
  @Getter
  public final DiscordNotifier notifier = new DiscordNotifier(System.getenv("WEB_HOOK_DISCORD"));

  @Override
  public String getBotUsername() {
    return System.getenv("BOT_USER_NAME");
  }

  @Override
  public String getBotToken() {
    return System.getenv("BOT_TOKEN_TELEGRAM");
  }

  private Long getGroupId() {
    return Long.parseLong(System.getenv("GROUP_TEST_ID"));
  }

  private String getUserId() {
    return System.getenv("USER_ID_TELEGRAM_1");
  }

  public static PkpmTelegramBot getInstance() {
    return instance;
  }

  public PkpmTelegramBot() {
    instance = this;
  }

  @Override
  public void onUpdateReceived(Update update) {
    Message message = update.getMessage();
    CallbackQuery callbackQuery = update.getCallbackQuery();

    Long chatId = message != null ? message.getChatId()
        : update.getCallbackQuery().getMessage().getChatId();
    if (verifyUserId(chatId)) {
      if (message != null && message.getChat().isUserChat() && message.hasText()) {
        selectAction(message, chatId);
      } else if (callbackQuery != null && update.hasCallbackQuery() && menuButton != null) {
        confirmSelection(callbackQuery, chatId);
      } else {
        createMessage(chatId, ChatMessage.INFORM_NOT_IDENTIFY_USER.getMessage());
      }
    }
  }

  private boolean verifyUserId(Long chatId) {
    return chatId.toString().equals(getUserId());
  }

  /**
   * Виконує вибір кнопки зі стартової клавіатури
   *
   * @param message
   * @param chatId
   */
  private void selectAction(Message message, Long chatId) {
    String messageText = message.getText();
    this.menuButton = checkWhichButtonFirstIsPress(chatId, messageText);
    createMenu(chatId, messageText);
    menuButtonAction(chatId, messageText);
    log.info("Status (menu button): {}, \"{}\", {}", chatId, messageText, menuButton);
  }

  /**
   * Перевіряє підтвердження і виконує сценарій
   *
   * @param callbackQuery
   * @param chatId
   */
  private void confirmSelection(CallbackQuery callbackQuery, Long chatId) {
    replyButton = callbackQuery.getData();
    Integer messageId = callbackQuery.getMessage().getMessageId();
    replyButtonAction(getGroupId(), chatId, messageId);
    log.info("Status (reply button) : {}, {}, {}, {}, {}", chatId, getGroupId(), menuButton,
        replyButton, messageId);
  }

  /**
   * Створює меню на початку роботи бота
   *
   * @param chatId  - id чату
   * @param message - текстове повідомлення
   */
  private void createMenu(Long chatId, String message) {
    if (menuButton == null) {
      sendMenu(chatId);
      input.put(chatId, message);
    }
  }

  /**
   * Метод аналізує яка кнопка із меню була натиснута і виконує відповідну логіку
   *
   * @param chatId  - id чату
   * @param message - текстове повідомлення
   */
  private void menuButtonAction(Long chatId, String message) {
    if (menuButton != null) {
      if (menuButton.equals(Buttons.BUTTON_1.getName()) && !message.equals(
          Buttons.BUTTON_1.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        compositeMessage = ChatMessage.INFORM_ADD_FOLDER.getMessage() + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButton.equals(Buttons.BUTTON_2.getName()) && !message.equals(
          Buttons.BUTTON_2.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        compositeMessage = ChatMessage.INFORM_ADD_POSITION.getMessage() + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButton.equals(Buttons.BUTTON_3.getName())) {
        sendReplyButtons(chatId, ChatMessage.INFORM_CHANGE_1.getMessage());
      }
    }
  }

  /**
   * Метод створює реакції при натисканні кнопок підтвердження
   *
   * @param groupId   - id групи
   * @param chatId    - id чату
   * @param messageId - id повідомлення
   */
  private void replyButtonAction(Long groupId, Long chatId, Integer messageId) {
    boolean isConfirm = Buttons.BUTTON_4.getName().equals(replyButton);
    boolean isReject = Buttons.BUTTON_5.getName().equals(replyButton);

    if (isConfirm || isReject) {
      if (menuButton.equals(Buttons.BUTTON_1.getName())) {
        handleButton(isConfirm, groupId, chatId, compositeMessage);
      }
      if (menuButton.equals(Buttons.BUTTON_2.getName())) {
        handleButton(isConfirm, groupId, chatId, compositeMessage);
      }
      if (menuButton.equals(Buttons.BUTTON_3.getName())) {
        handleButton(isConfirm, groupId, chatId, ChatMessage.INFORM_CHANGE_2.getMessage());
      }
    }
    clearState(chatId, messageId);
  }

  private void handleButton(boolean isConfirm, Long groupId, Long chatId,
      String sandMessageIfConfirm) {
    if (isConfirm) {
      sendAndNotify(groupId, chatId, sandMessageIfConfirm);
    } else {
      sendMessage(createMessage(chatId, ChatMessage.INFORM_REJECT.getMessage()));
    }
  }

  private void sendAndNotify(Long groupId, Long chatId, String message) {
    sendMessage(createMessage(groupId, message));
    notifier.sendMessage(message);
    sendMessage(createMessage(chatId, ChatMessage.INFORM_CONFIRM.getMessage()));
  }

  /**
   * Очищує стан кнопок меню і підтвердження
   *
   * @param chatId
   * @param messageId
   */
  private void clearState(Long chatId, Integer messageId) {
    menuButton = null;
    replyButton = null;
    try {
      execute(InlineKeyboardBuilder.removeKeyboard(chatId, messageId));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Метод перевіряє яка кнопка із меню натиснута і надсилає повідомлення для продовження діалогу
   *
   * @return - текстовий опис натиснутої кнопки
   */
  private String checkWhichButtonFirstIsPress(Long chatId, String messageText) {
    if (Buttons.BUTTON_1.getName().equals(messageText)) {
      sendMessage(createMessage(chatId, ChatMessage.INPUT_FOLDER.getMessage()));
      return messageText;
    } else if (Buttons.BUTTON_2.getName().equals(messageText)) {
      sendMessage(createMessage(chatId, ChatMessage.INPUT_POSITION.getMessage()));
      return messageText;
    } else if (Buttons.BUTTON_3.getName().equals(messageText)) {
      return messageText;
    } else if (menuButton != null) { // уточнити ??
      return menuButton;
    }
    return null;
  }

  /**
   * Метод створює додаткову клавіатуру
   *
   * @param chatId - id чата
   * @param text   - текст повідомлення
   */
  private void sendReplyButtons(Long chatId, String text) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(text).build();
    message.setReplyMarkup(InlineKeyboardBuilder.createSingleRowKeyboard(Buttons.BUTTON_4.getName(),
        Buttons.BUTTON_5.getName()));
    sendMessage(message);
  }

  /**
   * Метод створює меню з кнопок для відповідного чату
   *
   * @param chatId - id відповідного чату
   */
  private void sendMenu(Long chatId) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(ChatMessage.START.getMessage())
        .build();
    String[] firstRow = {Buttons.BUTTON_1.getName(), Buttons.BUTTON_2.getName()};
    String[] secondRow = {Buttons.BUTTON_3.getName()};

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
   * @param message - сформований меседж
   */
  private void sendMessage(SendMessage message) {
    try {
      execute(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public void sendMessageAndClearFile(Long chatId, String fileName) {
    List<String> messageList = MessageReader.read(fileName);
    if(!messageList.isEmpty()) {
      for (String message : messageList) {
        sendMessage(createMessage(chatId, message));
      }
      messageList.clear();
    }
  }
}