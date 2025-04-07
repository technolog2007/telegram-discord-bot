package pkpm.telegrambot.services.telegram;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class ReplyKeyboardBuilder {

  private ReplyKeyboardBuilder() {
    // Add a private constructor to hide the implicit public one.
  }

  private static KeyboardRow createRow(String... buttons) {
    KeyboardRow row = new KeyboardRow();
    for (String button : buttons) {
      row.add(button);
    }
    return row;
  }

  public static ReplyKeyboardMarkup createMultiRowKeyboard(List<String[]> buttons) {
    List<KeyboardRow> keyboard = new ArrayList<>();
    for (String[] button : buttons) {
      keyboard.add(createRow(button));
    }
    return ReplyKeyboardMarkup.builder().keyboard(keyboard).build();
  }

  public static ReplyKeyboardMarkup createSingleRowKeyboard(String... buttons) {
    return ReplyKeyboardMarkup.builder().keyboardRow(createRow(buttons)).build();
  }
}
