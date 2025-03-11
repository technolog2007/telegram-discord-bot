package pkpm.telegrambot.services;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class ReplyKeyboardBuilder {

  private static KeyboardRow createRow(String... buttons) {
    KeyboardRow row = new KeyboardRow();
    for (int i = 0; i < buttons.length; i++) {
      row.add(buttons[i]);
    }
    return row;
  }

  public static ReplyKeyboardMarkup createMultiRowKeyboard(List<String[]> buttons) {
    List<KeyboardRow> keyboard = new ArrayList<>();
    for (int i = 0; i < buttons.size(); i++) {
      keyboard.add(createRow(buttons.get(i)));
    }
    return ReplyKeyboardMarkup.builder().keyboard(keyboard).build();
  }

  public static ReplyKeyboardMarkup createSingleRowKeyboard(String... buttons) {
    return ReplyKeyboardMarkup.builder().keyboardRow(createRow(buttons)).build();
  }
}
