package pkpm.telegrambot.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageReader {

  public static List<String> read(String fileName) {
    List<String> messageList = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = reader.readLine()) != null) {
        messageList.add(line);
        log.info("{}",line);
      }
    } catch (IOException e) {
      log.warn("Cannot read file : " + fileName);
    }
    return messageList;
  }

}
