package pkpm.telegrambot.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageReader {

  private MessageReader(){
  }
  public static List<String> read(String fileName) {
    List<String> messageList = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = reader.readLine()) != null) {
        messageList.add(line);
        log.info("{}", line);
      }
    } catch (IOException e) {
      log.warn("Cannot read file : " + fileName);
    }
    return messageList;
  }

  public static void clean(String fileName) {
    try (FileWriter writer = new FileWriter(fileName)) {
      log.info("File recreated!");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
