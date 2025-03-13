package pkpm.telegrambot.models;

public enum Buttons {
  BUTTON_1("\uD83D\uDCC1 Нова вкладка"),
  BUTTON_2("\uD83D\uDCCB Нові позиції"),
  BUTTON_3("\uD83D\uDD04 Зміни"),
  BUTTON_4("✅ Підтвердити"),
  BUTTON_5("❌ Скасувати");
  private final String button;
  Buttons(String button) {

    this.button = button;
  }
  public String getName() {
    return button;
  }
}
