package pkpm.telegrambot.models;

public enum ChatMessage {
  START("⬇️ Натисніть відповідну кнопку:"),
  INFORM_CONFIRM("Повідомлення відправлено!\uD83D\uDE80"),
  INFORM_REJECT("Повідомлення не відправлено!\uD83D\uDEAB"),
  INFORM_PRE_SENT("Буде надіслано наступне повідомлення :"),
  INFORM_ADD_FOLDER("Додана нова вкладка : \""),
  INFORM_ADD_POSITION("Додані нові позиції на вкладку : \""),
  INFORM_CHANGE_1("Ви впевнені, що хочете внести зміни?"),
  INFORM_CHANGE_2("Додані нові позиції на вкладку \"Зміни\""),
  INPUT_FOLDER("Введіть позначення вкладки:"),
  INPUT_POSITION("Введіть позначення вкладки на яку додані позиції:");

  private final String message;

  ChatMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
