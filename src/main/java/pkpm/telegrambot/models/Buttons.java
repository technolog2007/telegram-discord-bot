package pkpm.telegrambot.models;

public enum Buttons {
  BUTTON_1("\uD83D\uDCC1 Нова вкладка"),
  BUTTON_2("\uD83D\uDCCB Нові позиції"),
  BUTTON_3("\uD83D\uDD04 Зміни"),
  BUTTON_4("✅ Підтвердити"),
  BUTTON_5("❌ Скасувати"),
  BUTTON_6("\uD83D\uDCC8 General Report"),
  BUTTON_7("\uD83D\uDCCA Employee Report");
  private final String button;
  Buttons(String button) {

    this.button = button;
  }
  public String getName() {
    return button;
  }
}
