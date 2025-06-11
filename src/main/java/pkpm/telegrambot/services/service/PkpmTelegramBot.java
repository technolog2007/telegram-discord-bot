package pkpm.telegrambot.services.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pkpm.company.automation.models.Employees;
import pkpm.telegrambot.models.ButtonAction;
import pkpm.telegrambot.models.Buttons;
import pkpm.telegrambot.models.ChatMessage;
import pkpm.telegrambot.services.discord.DiscordNotifier;
import pkpm.telegrambot.utils.InlineKeyboardBuilder;
import pkpm.telegrambot.utils.MessageReader;
import pkpm.telegrambot.utils.ReplyKeyboardBuilder;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private final String botUsername;
  private final String botToken;
  private final Long groupId;
  private final List<String> verifiedUsers;
  private final String graphName;
  private final ReportService reportService;
  @Getter
  public final DiscordNotifier notifier;
  private final Map<Long, ButtonAction> userStates = new ConcurrentHashMap<>();

  public PkpmTelegramBot(String botUsername, String botToken, Long groupId, String usersListString,
      String graphName, DiscordNotifier notifier, ReportService reportService) {
    this.botUsername = botUsername;
    this.botToken = botToken;
    this.groupId = groupId;
    this.verifiedUsers = List.of(usersListString.split(";"));
    this.graphName = graphName;
    this.notifier = notifier;
    this.reportService = reportService;
  }

  @Override
  public String getBotUsername() {
    return this.botUsername;
  }

  @Override
  public String getBotToken() {
    return this.botToken;
  }

  private Long getGroupId() {
    return this.groupId;
  }

  private List<String> getVerifyUsersIdList() {
    return this.verifiedUsers;
  }

  /**
   * Верифікує користувача для роботи з ботом
   *
   * @param chatId - id чата
   * @return - булеве значення результату верифікації
   */
  private boolean verifyUsersIdList(Long chatId) {
    return this.verifiedUsers.contains(chatId.toString());
  }

  @Override
  public void onUpdateReceived(Update update) {
    Long chatId;
    if (update.hasMessage() && update.getMessage().getChatId() != null) {
      chatId = update.getMessage().getChatId();
    } else if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null
        && update.getCallbackQuery().getMessage().getChatId() != null) {
      chatId = update.getCallbackQuery().getMessage().getChatId();
    } else {
      log.warn("Received update without chat ID: {}", update); // Логуємо попередження
      return;
    }

    if (!verifyUsersIdList(chatId)) {
      sendMessage(createMessage(chatId, ChatMessage.INFORM_NOT_IDENTIFY_USER.getMessage()));
      return;
    }

    ButtonAction currentUserAction = userStates.computeIfAbsent(chatId, k -> new ButtonAction());

    if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getChat()
        .isUserChat()) {
      selectMenuAction(update.getMessage().getText(), chatId, currentUserAction);
    } else if (update.hasCallbackQuery()) {
      String pressButton = update.getCallbackQuery().getData();
      if (currentUserAction.getButton() != null) {
        if (checkPressButtonIsConfirm(pressButton)) {
          confirmSelection(update.getCallbackQuery(), chatId, currentUserAction);
        } else if (checkPressButtonIsEmployee(pressButton)) {
          createReportEmployeesAndSendMessage(chatId, graphName, pressButton);
        }
        userStates.remove(chatId);
      } else {
        log.warn("User {} pressed callback button '{}' without active dialogue state. Ignoring.",
            chatId, pressButton);
        sendMenu(chatId);
      }
    } else {
      log.info("Received unsupported update type or message: {}", update);
      sendMenu(chatId);
    }
  }

  private void selectMenuAction(String messageText, Long chatId, ButtonAction currentUserAction) {
    if (checkMessageIsButton(messageText)) {
      log.info("Натиснута кнопка ...");
      Buttons button = checkWhichButtonPressed(messageText);
      currentUserAction.setButton(button);
      sendAdditionalMessage(chatId, currentUserAction);
      if (!currentUserAction.isAdditionalDialogue()) {
        log.info("Запущена дія кнопки ...");
        makeButtonAction(chatId, messageText, currentUserAction);
      }
    } else if (currentUserAction != null && currentUserAction.isAdditionalDialogue()) {
      log.info("Запущена дія кнопки з additional ...");
      currentUserAction.setMessageDate(messageText);
      makeButtonAction(chatId, messageText, currentUserAction);
    } else {
      log.info("Вивід меню ...");
      userStates.remove(chatId);
      sendMenu(chatId);
    }
    log.info("Current sum status: {}, \"{}\", {}", chatId, messageText, currentUserAction);
  }

  private boolean checkMessageIsButton(String messageText) {
    return Arrays.stream(Buttons.values())
        .anyMatch(button -> button.getName().equals(messageText));
  }

  private Buttons checkWhichButtonPressed(String messageText) {
    return Arrays.stream(Buttons.values())
        .filter(button -> button.getName().equals(messageText))
        .findFirst()
        .orElse(null);
  }

  private void sendAdditionalMessage(Long chatId, ButtonAction currentUserAction) {
    switch (currentUserAction.getButton()) {
      case BUTTON_1 -> {
        sendMessage(createMessage(chatId, ChatMessage.INPUT_FOLDER.getMessage()));
        currentUserAction.setAdditionalDialogue(true);
      }
      case BUTTON_2 -> {
        sendMessage(createMessage(chatId, ChatMessage.INPUT_POSITION.getMessage()));
        currentUserAction.setAdditionalDialogue(true);
      }
      default -> log.info("Це натискання не потребує додаткового повідомлення!");
    }
  }

  /**
   * Аналізує яка кнопка із меню була натиснута і виконує відповідну логіку
   *
   * @param chatId  - id чату
   * @param message - текстове повідомлення
   */
  private void makeButtonAction(Long chatId, String message, ButtonAction currentUserAction) {
    switch (currentUserAction.getButton()) {
      case BUTTON_1 -> {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        currentUserAction.setCompositeMessage(
            ChatMessage.INFORM_ADD_FOLDER.getMessage() + message + "\"");
        sendReplyButtons(chatId, currentUserAction.getCompositeMessage());
      }
      case BUTTON_2 -> {
        sendMessage(createMessage(chatId, ChatMessage.INFORM_PRE_SENT.getMessage()));
        currentUserAction.setCompositeMessage(
            ChatMessage.INFORM_ADD_POSITION.getMessage() + message + "\"");
        sendReplyButtons(chatId, currentUserAction.getCompositeMessage());
      }
      case BUTTON_3 -> {
        sendReplyButtons(chatId, ChatMessage.INFORM_CHANGE_1.getMessage());
      }
      case BUTTON_6 -> {
        log.info("Формую загальний звіт та вивожу в чат telegram:");
        createReportGeneralAndSendMessage(chatId, graphName);
        userStates.remove(chatId);
      }
      case BUTTON_7 -> {
        log.info("Формую звіт по виконавцям та вивожу в чат telegram:");
        sendInlineEmployeesButtons(chatId, "Оберіть виконавця \uD83D\uDC47");
      }
      default -> log.info("Something wrong with menuButtonAction()!");
    }
  }

  /**
   * Перевіряє чи була натиснута кнопка підтвердження
   *
   * @param pressButton - натиснута інлайн кнопка
   * @return - true - якщо натиснута кнопка підтвердження; false - якщо натиснута інша кнопка
   */
  private boolean checkPressButtonIsConfirm(String pressButton) {
    return pressButton.equals(Buttons.BUTTON_4.getName()) || pressButton.equals(
        Buttons.BUTTON_5.getName());
  }

  /**
   * Перевіряє чи була натиснута кнопка вибору співробітника
   *
   * @param pressButton - натиснута інлайн кнопка
   * @return - true - якщо натиснута кнопка вибору працівника; false - якщо натиснута інша кнопка
   */
  private boolean checkPressButtonIsEmployee(String pressButton) {
    return pressButton.equals(Employees.EMPLOYEE_1.getName()) ||
        pressButton.equals(Employees.EMPLOYEE_2.getName()) ||
        pressButton.equals(Employees.EMPLOYEE_3.getName()) ||
        pressButton.equals(Employees.EMPLOYEE_4.getName()) ||
        pressButton.equals(Employees.EMPLOYEE_5.getName());
  }

  private void confirmSelection(CallbackQuery callbackQuery, Long chatId,
      ButtonAction currentUserAction) {
    currentUserAction.setReplyButton(callbackQuery.getData());
    Integer messageId = callbackQuery.getMessage().getMessageId();
    replyButtonAction(getGroupId(), chatId, messageId, currentUserAction);
    log.info("Status (reply button) : {}, {}, {}, {}", chatId, getGroupId(), currentUserAction,
        messageId);
  }

  private void createReportGeneralAndSendMessage(Long chatId, String graphName) {
    String report = reportService.createGeneralReport(graphName);
    sendMessage(createMessage(chatId, report));
  }

  private void createReportEmployeesAndSendMessage(Long chatId, String graphName, String employee) {
    String report = reportService.createEmployeeReport(graphName, employee);
    sendMessage(createMessage(chatId, report));
  }

  /**
   * Створює реакції при натисканні кнопок підтвердження
   *
   * @param groupId   - id групи
   * @param chatId    - id чату
   * @param messageId - id повідомлення
   */
  private void replyButtonAction(Long groupId, Long chatId, Integer messageId,
      ButtonAction currentUserAction) {
    boolean isConfirm = Buttons.BUTTON_4.getName().equals(currentUserAction.getReplyButton());
    boolean isReject = Buttons.BUTTON_5.getName().equals(currentUserAction.getReplyButton());

    if (isConfirm || isReject) {
      switch (currentUserAction.getButton()) {
        case BUTTON_1, BUTTON_2 -> {
          handleButton(isConfirm, groupId, chatId, currentUserAction.getCompositeMessage());
        }
        case BUTTON_3 ->
            handleButton(isConfirm, groupId, chatId, ChatMessage.INFORM_CHANGE_2.getMessage());
        default -> log.info("Something wrong with replyButtonAction()!");
      }
      clearState(chatId, messageId);
      sendMenu(chatId);
    }

  }

  /**
   * Перевіряє значення кнопки підтвердження і, якщо підтвердження: - true - відсилає відповідне
   * повідомлення в групу і чат бота, - false - відсилає інформаційне повідомлення в чат бота
   *
   * @param isConfirm - булеве значення кнопки підтвердження
   * @param groupId   - id групи
   * @param chatId    - id чату
   */
  private void handleButton(boolean isConfirm, Long groupId, Long chatId,
      String sendMessage) {
    if (isConfirm) {
      sendAndNotify(groupId, chatId, sendMessage);
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
   * @param chatId    -
   * @param messageId -
   */
  private void clearState(Long chatId, Integer messageId) {
    userStates.remove(chatId);
    log.info("Clear buttons state successful!");
    try {
      execute(InlineKeyboardBuilder.removeKeyboard(chatId, messageId));
    } catch (TelegramApiException e) {
      log.error("Failed to remove keyboard for chatId {}: {}", chatId, e.getMessage(), e);
    }
  }

  /**
   * Створює додаткову клавіатуру
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

  private void sendInlineEmployeesButtons(Long chatId, String text) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(text).build();
    List<String> buttons = Arrays.stream(Employees.values()).map(Employees::getName).toList();
    InlineKeyboardMarkup ikm = InlineKeyboardBuilder.createColumnKeyboard(buttons);
    message.setReplyMarkup(ikm);
    sendMessage(message);
  }

  /**
   * Створює меню з кнопок для відповідного чату
   *
   * @param chatId - id відповідного чату
   */
  private void sendMenu(Long chatId) {
    SendMessage message = SendMessage.builder().chatId(chatId).text(ChatMessage.START.getMessage())
        .build();
    String[] firstRow = {Buttons.BUTTON_1.getName(), Buttons.BUTTON_2.getName(),
        Buttons.BUTTON_3.getName()};
    String[] secondRow = {Buttons.BUTTON_6.getName(), Buttons.BUTTON_7.getName()};

    ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardBuilder.createMultiRowKeyboard(
        List.of(firstRow, secondRow));
    keyboardMarkup.setResizeKeyboard(true);
    keyboardMarkup.setOneTimeKeyboard(false);
    keyboardMarkup.setSelective(true);

    message.setReplyMarkup(keyboardMarkup);
    sendMessage(message);
  }

  /**
   * Створює і надсилає текстове повідомлення в чат
   *
   * @param chatId  - id чата
   * @param message - повідомлення
   */
  private SendMessage createMessage(Long chatId, String message) {
    return SendMessage.builder().chatId(chatId).text(message).build();
  }

  /**
   * Створює і надсилає у відповідний чат повідомлення
   *
   * @param message - сформований меседж
   */
  private void sendMessage(SendMessage message) {
    try {
      execute(message);
    } catch (TelegramApiException e) {
      log.error("Failed to send message to chatId {}: {}", message.getChatId(), e.getMessage(), e);
    }
  }

  /**
   * Зчитує повідомлення з файлу і відправляє їх по черзі в чат групи телеграма по id і в групу
   * discord через webhook
   *
   * @param chatId   - id групи
   * @param fileName - повне ім'я файла
   */
  public void sendMessageAndCleanFile(Long chatId, String fileName) {
    List<String> messageList = MessageReader.read(fileName);
    if (!messageList.isEmpty()) {
      for (String message : messageList) {
        sendMessage(createMessage(chatId, message));
        notifier.sendMessage(message);
      }
      MessageReader.clean(fileName);
    }
  }
}