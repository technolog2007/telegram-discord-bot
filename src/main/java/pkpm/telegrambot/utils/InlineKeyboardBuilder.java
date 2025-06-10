package pkpm.telegrambot.utils;

import java.util.ArrayList;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public class InlineKeyboardBuilder {

  /**
   * Creates buttons for the inline keyboard
   *
   * @param name - button name
   * @return - InlineKeyboardButton which can be used to create an inline keyboard
   */
  private static InlineKeyboardButton createButton(String name, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(name);
    button.setCallbackData(callbackData);
    return button;
  }

  /**
   * Creates a keyboard row of buttons.
   *
   * @param buttons - An array of strings, where each string represents the button text and
   *                callbackData. The array must have an even number of elements (text,
   *                callbackData).
   * @return - List<InlineKeyboardButton> contains buttons belonging to the same row
   */
  private static List<InlineKeyboardButton> createRow(String... buttons) {
    List<InlineKeyboardButton> row = new ArrayList<>();
    for (int i = 0; i < buttons.length; i++) {
      InlineKeyboardButton button = createButton(buttons[i], buttons[i]);
      row.add(button);
    }
    return row;
  }

  /**
   * Creates an InlineKeyboardMarkup from a given list of button strings
   *
   * @param buttons - List<List<InlineKeyboardButton>> where each inner list represents a row of
   *                buttons
   * @return - InlineKeyboardMarkup with a given list of button rows
   */
  private static InlineKeyboardMarkup createInlineKeyboard(
      List<List<InlineKeyboardButton>> buttons) {
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(buttons);
    return markup;
  }

  /**
   * Creates an InlineKeyboardMarkup with a single row of buttons.
   *
   * @param buttons - An array of strings, where each string represents the button text and
   *                callbackData. The array must have an even number of elements (text,
   *                callbackData).
   * @return - InlineKeyboardMarkup with one row of buttons
   */
  public static InlineKeyboardMarkup createSingleRowKeyboard(String... buttons) {
    return createInlineKeyboard(List.of(createRow(buttons)));
  }

  /**
   * Creates an InlineKeyboardMarkup with multiple rows of buttons
   *
   * @param buttons - List<String[]> - contains rows with buttons
   * @return - InlineKeyboardMarkup which creates a multi row`s keyboard
   */
  public static InlineKeyboardMarkup createMultiRowKeyboard(List<String[]> buttons) {
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int i = 0; i < buttons.size(); i++) {
      keyboard.add(createRow(buttons.get(i)));
    }
    return createInlineKeyboard(keyboard);
  }

  /**
   * Removes the inline keyboard from the message
   *
   * @param chatId    - ID of the chat in which the message is located
   * @param messageId - ID of the message from which you want to remove the keyboard
   * @return - EditMessageReplyMarkup which can be used to remove the keyboard
   */
  public static EditMessageReplyMarkup removeKeyboard(Long chatId, Integer messageId) {
    EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
    editMarkup.setChatId(chatId);
    editMarkup.setMessageId(messageId);
    editMarkup.setReplyMarkup(null);
    return editMarkup;
  }

  public static InlineKeyboardMarkup createColumnKeyboard(List<String> buttons) {
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    for (int i = 0; i < buttons.size(); i++) {
      keyboard.add(createRow(buttons.get(i)));
    }
    return createInlineKeyboard(keyboard);
  }

}