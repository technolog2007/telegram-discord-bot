package pkpm.telegrambot.services.telegram;

import java.util.Arrays;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pkpm.company.automation.models.Employees;
import pkpm.company.automation.models.ReportEmployee;
import pkpm.company.automation.models.ReportGeneral;
import pkpm.company.automation.services.GraphExecutionReport;
import pkpm.company.automation.services.MakeSnapshot;
import pkpm.telegrambot.models.Buttons;
import pkpm.telegrambot.models.ChatMessage;
import pkpm.telegrambot.services.discord.DiscordNotifier;
import pkpm.telegrambot.utils.MessageReader;

@Slf4j
public class PkpmTelegramBot extends TelegramLongPollingBot {

  private static final String GRAPH_NAME = "Y:\\Графіки роботи ВТВС\\График выдачи докуметации.xlsx";
  @Getter
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
        String pressButton = callbackQuery.getData();
        if (checkPressButtonIsConfirm(pressButton)) {
          confirmSelection(callbackQuery, chatId);
        } else if (checkPressButtonIsEmployee(pressButton)) {
          createReportEmployeesAndSendMessage(chatId, GRAPH_NAME, pressButton);
        }
      } else {
        createMessage(chatId, ChatMessage.INFORM_NOT_IDENTIFY_USER.getMessage());
      }
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
   * Аналізує яка кнопка із меню була натиснута і виконує відповідну логіку
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
      if (menuButton.equals(Buttons.BUTTON_6.getName())) {
        log.warn("Формую загальний звіт та вивожу в чат telegram:");
        createReportGeneralAndSendMessage(chatId, GRAPH_NAME);
        this.menuButton = null;
      } else if (menuButton.equals(Buttons.BUTTON_7.getName())) {
        log.warn("Формую звіт по виконавцям та вивожу в чат telegram:");
        sendInlineEmployeesButtons(chatId, "Оберіть виконавця \uD83D\uDC47");
      }
    }
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
    String report = executionReport.writeResulForReportEmployeetToString(result.get(Employees.fromName(employee)));
    this.menuButton = null;
    sendMessage(createMessage(chatId, report));
  }

  /**
   * Створює реакції при натисканні кнопок підтвердження
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

  /**
   * Перевіряє значення кнопки підтвердження і, якщо підтвердження: - true - відсилає відповідне
   * повідомлення в групу і чат бота, - false - відсилає інформаційне повідомлення в чат бота
   *
   * @param isConfirm            - булеве значення кнопки підтвердження
   * @param groupId              - id групи
   * @param chatId               - id чату
   * @param sandMessageIfConfirm - сформоване повідомлення для розсилки
   */
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
   * @param chatId    -
   * @param messageId -
   */
  private void clearState(Long chatId, Integer messageId) {
    menuButton = null;
    replyButton = null;
    try {
      execute(InlineKeyboardBuilder.removeKeyboard(chatId, messageId));
    } catch (TelegramApiException e) {
      log.warn("You have exception, when you try send message!");
    }
  }

  /**
   * Перевіряє яка кнопка із меню натиснута і надсилає повідомлення для продовження діалогу
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
    } else if (Buttons.BUTTON_6.getName().equals(messageText)) {
      return messageText;
    } else if (Buttons.BUTTON_7.getName().equals(messageText)) {
      return messageText;
    } else if (menuButton != null) {
      return menuButton;
    }
    return null;
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