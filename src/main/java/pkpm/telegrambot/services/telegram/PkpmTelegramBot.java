package pkpm.telegrambot.services.telegram;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pkpm.company.automation.models.Employees;
import pkpm.company.automation.models.ReportEmployee;
import pkpm.company.automation.models.ReportGeneral;
import pkpm.company.automation.services.GraphExecutionReport;
import pkpm.company.automation.services.MakeSnapshot;
import pkpm.telegrambot.models.ButtonAction;
import pkpm.telegrambot.models.Buttons;
import pkpm.telegrambot.models.ChatMessage;
import pkpm.telegrambot.services.discord.DiscordNotifier;
import pkpm.telegrambot.utils.MessageReader;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private static final String GRAPH_NAME = System.getenv("GRAPH_NAME");
  @Getter
  public final DiscordNotifier notifier = new DiscordNotifier(System.getenv("WEB_HOOK_DISCORD"));
  private final Map<Long, ButtonAction> userStates = new ConcurrentHashMap<>();

  @Override
  public String getBotUsername() {
    return System.getenv("BOT_USER_NAME");
  }

  @Override
  public String getBotToken() {
    return System.getenv("BOT_TOKEN_TELEGRAM");
  }

  /**
   * Повертає значення id групи приведене до типу long із конфігураційного файлу
   *
   * @return - id групи приведене до long
   */
  private Long getGroupId() {
    return Long.parseLong(System.getenv("GROUP_TEST_ID"));
  }

  private List<String> getVerifyUsersIdList() {
    return List.of(System.getenv("USERS_LIST").split(";"));
  }

  /**
   * Верифікує користувача для роботи з ботом
   *
   * @param chatId - id чата
   * @return - булеве значення результату верифікації
   */
  private boolean verifyUserId(Long chatId) {
    List<String> userList = getVerifyUsersIdList();
    for (String user : userList) {
      if (user.equals(chatId.toString())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onUpdateReceived(Update update) {
    Message message = update.getMessage();
    CallbackQuery callbackQuery = update.getCallbackQuery();
    Long chatId = message != null ? message.getChatId()
        : update.getCallbackQuery().getMessage().getChatId();
    ButtonAction currentUserAction = userStates.computeIfAbsent(chatId, k -> new ButtonAction());
    if (verifyUserId(chatId)) {
      if (message != null && message.getChat().isUserChat() && message.hasText()) {
        selectMenuAction(message.getText(), chatId, currentUserAction);
      } else if (callbackQuery != null && update.hasCallbackQuery()
          && userStates.get(chatId) != null) {
        String pressButton = callbackQuery.getData();
        if (checkPressButtonIsConfirm(pressButton)) {
          confirmSelection(callbackQuery, chatId, currentUserAction);
        } else if (checkPressButtonIsEmployee(pressButton)) {
          createReportEmployeesAndSendMessage(chatId, GRAPH_NAME, pressButton);
        }
        userStates.remove(chatId);
      }
    } else {
      createMessage(chatId, ChatMessage.INFORM_NOT_IDENTIFY_USER.getMessage());
    }
  }

  private void selectMenuAction(String messageText, Long chatId, ButtonAction currentUserAction) {
    if (checkMessageIsButton(messageText)) {
      log.warn("Натиснута кнопка ...");
      Buttons button = checkWhichButtonPressed(messageText);
      currentUserAction.setButton(button);
      sendAdditionalMessage(chatId, currentUserAction);
      if (!currentUserAction.isAdditionalDialogue()) {
        log.warn("Запущена дія кнопки ...");
        makeButtonAction(chatId, messageText, currentUserAction);
      }
    } else if (currentUserAction != null && currentUserAction.isAdditionalDialogue()) {
      log.warn("Запущена дія кнопки з additional ...");
      currentUserAction.setMessageDate(messageText);
      makeButtonAction(chatId, messageText, currentUserAction);
    } else {
      log.warn("Вивід меню ...");
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
      default -> log.error("Це натискання не потребує додаткового повідомлення!");
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
        log.warn("Формую загальний звіт та вивожу в чат telegram:");
        createReportGeneralAndSendMessage(chatId, GRAPH_NAME);
        userStates.remove(chatId);
      }
      case BUTTON_7 -> {
        log.warn("Формую звіт по виконавцям та вивожу в чат telegram:");
        sendInlineEmployeesButtons(chatId, "Оберіть виконавця \uD83D\uDC47");
        userStates.remove(chatId);
      }
      default -> log.error("Something wrong with menuButtonAction()!");
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

  /**
   * Перевіряє підтвердження і виконує сценарій
   *
   * @param callbackQuery
   * @param chatId
   */
  private void confirmSelection(CallbackQuery callbackQuery, Long chatId,
      ButtonAction currentUserAction) {
    currentUserAction.setReplyButton(callbackQuery.getData());
    Integer messageId = callbackQuery.getMessage().getMessageId();
    replyButtonAction(getGroupId(), chatId, messageId, currentUserAction);
    log.info("Status (reply button) : {}, {}, {}, {}", chatId, getGroupId(), currentUserAction,
        messageId);
  }

  /**
   * Формує звіт, щодо загального виконання графіка по кожній вкладці
   *
   * @param graphName - файл графіка
   */
  private void createReportGeneralAndSendMessage(Long chatId, String graphName) {
    GraphExecutionReport executionReport = new GraphExecutionReport();
    List<ReportGeneral> listOfResults = executionReport.getDateForGeneralReport(
        new MakeSnapshot(graphName).getBs());
    String report = executionReport.writeResultToString(listOfResults);
    sendMessage(createMessage(chatId, report));
  }

  private void createReportEmployeesAndSendMessage(Long chatId, String graphName, String employee) {
    GraphExecutionReport executionReport = new GraphExecutionReport();
    Map<Employees, List<ReportEmployee>> result = executionReport.getListOfEmployeesReports(
        new MakeSnapshot(graphName).getBs());
    String report = executionReport.writeResulForReportEmployeetToString(
        result.get(Employees.fromName(employee)));
    sendMessage(createMessage(chatId, report));
//    this.buttonAction = null;
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
        default -> log.error("Something wrong with replyButtonAction()!");
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
//    this.buttonAction = null;
//    this.replyButton = null;
    log.info("Clear buttons state successful!");
    try {
      execute(InlineKeyboardBuilder.removeKeyboard(chatId, messageId));
    } catch (TelegramApiException e) {
      log.warn("You have exception, when you try send message!");
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
      log.warn("You have exception, when you try send message!");
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