package pkpm.telegrambot.models;

import lombok.Getter;

@Getter
public enum ChatMessage {

  START("⬇️ Натисніть відповідну кнопку:"),
  INFORM_CONFIRM("Повідомлення відправлено!\uD83D\uDE80"),
  INFORM_REJECT("Повідомлення не відправлено!\uD83D\uDEAB"),
  INFORM_PRE_SENT("\uD83D\uDD14   Буде надіслано наступне повідомлення :"),
  INFORM_ADD_FOLDER("➕\uD83D\uDCC1 Додана нова вкладка \uD83D\uDC49 \""),
  INFORM_ADD_POSITION("➕\uD83D\uDCC4 Додані нові позиції на вкладку \uD83D\uDC49 \""),
  INFORM_CHANGE_1("Ви впевнені, що хочете внести зміни❓"),
  INFORM_CHANGE_2("➕\uD83D\uDCC4 Додані нові позиції на вкладку \uD83D\uDC49 \"Зміни\""),
  INPUT_FOLDER("✏\uFE0F Введіть позначення вкладки:"),
  INPUT_POSITION("✏\uFE0F Введіть позначення вкладки на яку додані позиції:"),
  INFORM_NOT_IDENTIFY_USER(
      "✏\uFE0F Ви не маєте права на роботу з ботом! \nЗверніться до адміністратора!"),
  DISCORD_RESPONSE_COD_204("Message successfully sent to Discord!\uD83D\uDE80");

  private final String message;

  ChatMessage(String message) {
    this.message = message;
  }

}
