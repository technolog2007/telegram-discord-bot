package pkpm.telegrambot.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import pkpm.telegrambot.models.Buttons;
import pkpm.telegrambot.models.ChatMessage;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private final Map<Long, String> input = new HashMap<>();
  private String menuButtonPres = null;
  private String replyButtonPress = null;
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
    Long chatId = update.getMessage() != null ? update.getMessage().getChatId()
        : update.getCallbackQuery().getMessage().getChatId();

    if (update.hasCallbackQuery()) {
      replyButtonPress = update.getCallbackQuery().getData();
      Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
      replyButtonReaction(getGroupId(), chatId, messageId);
      log.info("Status (reply button) : {}, {}, {}, {}, {}", chatId, getGroupId(), menuButtonPres,
          replyButtonPress, messageId);
    } else if (update.getMessage().getChat().isUserChat() && update.getMessage().hasText()) {
      String message = update.getMessage().getText();
      boolean flagUserChat = update.getMessage().getChat().isUserChat();
      boolean flagHasText = update.getMessage().hasText();
      menuButtonPres = checkWhichButtonFirstIsPress(chatId, message, flagUserChat);
      createMenu(flagUserChat, flagHasText, chatId, message);
      menuPressButtonReaction(chatId, flagUserChat, flagHasText, message);
      log.info("Status (menu button): {}, \"{}\", {}", chatId, message, menuButtonPres);
    }
  }

  /**
   * Метод створює меню на початку роботи бота
   *
   * @param flagUserChat - підтвердження чату з ботом
   * @param flagHasText - чи є текстове сповіщення в чаті
   * @param chatId - id чату
   * @param message - текстове повідомлення
   */
  private void createMenu(boolean flagUserChat, boolean flagHasText, Long chatId, String message) {
    if (flagUserChat && flagHasText && menuButtonPres == null) {
      sendMenu(chatId);
      input.put(chatId, message);
    }
  }

  /**
   * Метод аналізує яка кнопка із меню була натиснута і виконує відповідну логіку
   *
   * @param chatId - id чату
   * @param flagUserChat - підтвердження чату з бото
   * @param flagHasText - чи є текстове сповіщення в чаті
   * @param message - текстове повідомлення
   */
  private void menuPressButtonReaction(Long chatId, boolean flagUserChat, boolean flagHasText,
      String message) {
    if (flagUserChat && flagHasText && menuButtonPres != null) {
      if (menuButtonPres.equals(Buttons.BUTTON_1.getName()) && !message.equals(Buttons.BUTTON_1.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        compositeMessage = ChatMessage.INFORM_ADD_FOLDER.getMessage() + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButtonPres.equals(Buttons.BUTTON_2.getName()) && !message.equals(Buttons.BUTTON_2.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        compositeMessage = ChatMessage.INFORM_ADD_POSITION.getMessage() + message + "\"";
        sendReplyButtons(chatId, compositeMessage);
      }
      if (menuButtonPres.equals(Buttons.BUTTON_3.getName())) {
        sendReplyButtons(chatId, ChatMessage.INFORM_CHANGE_1.getMessage());
      }
    }
  }

  /**
   * Метод створює рекції при натисканні кнопок підтвердження
   *
   * @param groupId - id групи
   * @param chatId - id чату
   * @param messageId - id повідомлення
   */
  private void replyButtonReaction(Long groupId, Long chatId, Integer messageId) {
    if (menuButtonPres != null) {
      if (menuButtonPres.equals(Buttons.BUTTON_1.getName()) && replyButtonPress.equals(Buttons.BUTTON_4.getName())) {
        sendMessage(createMessage(groupId, compositeMessage));
        notifier.sendMessage(compositeMessage);
        sendMessage(createMessage(chatId, ChatMessage.INFORM_CONFIRM.getMessage()));
      } else if (menuButtonPres.equals(Buttons.BUTTON_1.getName()) && replyButtonPress.equals(Buttons.BUTTON_5.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_REJECT.getMessage()));
      }
      if (menuButtonPres.equals(Buttons.BUTTON_2.getName()) && replyButtonPress.equals(Buttons.BUTTON_4.getName())) {
        sendMessage(createMessage(groupId, compositeMessage));
        notifier.sendMessage(compositeMessage);
        sendMessage(createMessage(chatId, ChatMessage.INFORM_CONFIRM.getMessage()));
      } else if (menuButtonPres.equals(Buttons.BUTTON_2.getName()) && replyButtonPress.equals(Buttons.BUTTON_5.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_REJECT.getMessage()));
      }
      if (menuButtonPres.equals(Buttons.BUTTON_3.getName()) && replyButtonPress.equals(Buttons.BUTTON_4.getName())) {
        sendMessage(createMessage(groupId, ChatMessage.INFORM_CHANGE_2.getMessage()));
        notifier.sendMessage(ChatMessage.INFORM_CHANGE_2.getMessage());
        sendMessage(createMessage(chatId, ChatMessage.INFORM_CONFIRM.getMessage()));
      } else if (menuButtonPres.equals(Buttons.BUTTON_3.getName()) && replyButtonPress.equals(Buttons.BUTTON_5.getName())) {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_REJECT.getMessage()));
      }
      menuButtonPres = null;
      replyButtonPress = null;
      try {
        execute(InlineKeyboardBuilder.removeKeyboard(chatId, messageId));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Метод перевіряє яка кнопка із меню натиснута і надсилає повідомлення для продовження діалогу
   *
   * @param flagUserChat - підтвердження чату з бото
   * @return - текстовий опис натиснутої кнопки
   */
  private String checkWhichButtonFirstIsPress(Long chatId, String messageText,
      boolean flagUserChat) {
    if (flagUserChat && Buttons.BUTTON_1.getName().equals(messageText)) {
      sendMessage(createMessage(chatId, ChatMessage.INPUT_FOLDER.getMessage()));
      return messageText;
    } else if (flagUserChat && Buttons.BUTTON_2.getName().equals(messageText)) {
      sendMessage(createMessage(chatId, ChatMessage.INPUT_POSITION.getMessage()));
      return messageText;
    } else if (flagUserChat && Buttons.BUTTON_3.getName().equals(messageText)) {
      return messageText;
    } else if (menuButtonPres != null) {
      return menuButtonPres;
    }
    return null;
  }

  /**
   * Мтеод створює додаткову клавіатуру
   *
   * @param chatId - id чата
   * @param text   - текст повідомлення
   */
  private void sendReplyButtons(Long chatId, String text) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(text).build();
    message.setReplyMarkup(InlineKeyboardBuilder.createSingleRowKeyboard(Buttons.BUTTON_4.getName(), Buttons.BUTTON_5.getName()));
    sendMessage(message);
  }

  /**
   * Метод створює меню з кнопок для відповідного чату
   *
   * @param chatId - id відповідного чату
   */
  private void sendMenu(Long chatId) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(ChatMessage.START.getMessage()).build();
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
}