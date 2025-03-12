package pkpm.telegrambot.services;

import java.util.ArrayList;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public class InlineKeyboardBuilder {

  private static InlineKeyboardMarkup createInlineKeyboard(
      List<List<InlineKeyboardButton>> buttons) {
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(buttons);
    return markup;
  }

  private static InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
  }

  private static List<InlineKeyboardButton> createRow(InlineKeyboardButton... buttons) {
    return List.of(buttons);
  }

  public static InlineKeyboardMarkup createMultiRowKeyboard(List<String[]> buttons) {
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int i = 0; i < buttons.size(); i++) {
      keyboard.add(createRow(buttons.get(i)));
    }
    return createInlineKeyboard(keyboard);
  }

  public static InlineKeyboardMarkup createSingleRowKeyboard(String... buttons) {
    return createInlineKeyboard(List.of(createRow(buttons)));
  }

  private static List<InlineKeyboardButton> createRow(String... buttons) {
    List<InlineKeyboardButton> row = new ArrayList<>();
    for (int i = 0; i < buttons.length; i++) {
      InlineKeyboardButton button = createButton(buttons[i], buttons[i]);
      row.add(button);
    }
    return row;
  }

  public static EditMessageReplyMarkup removeKeyboard(Long chatId, Integer messageId){
    EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
    editMarkup.setMessageId(messageId);
    editMarkup.setReplyMarkup(null);
    return editMarkup;
  }


}