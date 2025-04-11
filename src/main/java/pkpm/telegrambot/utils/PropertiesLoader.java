package pkpm.telegrambot.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertiesLoader {

  private final Properties properties = new Properties();
  private final String PATH = "src/main/resources/";
  private final String FILE_PROPERTIES = "app.properties";

  public PropertiesLoader() {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(FILE_PROPERTIES)) {
      if (input == null) {
        loadFromProject(PATH, FILE_PROPERTIES);
      } else {
        properties.load(input);
      }
    } catch (IOException ex) {
      log.warn("Sorry, unable to find app.properties!\n{}", ex.getMessage());
    }
  }

  private void loadFromProject(String path, String fileProperties) {
    try (FileInputStream inputStream = new FileInputStream(PATH + FILE_PROPERTIES)) {
      properties.load(inputStream);
    } catch (IOException exception) {
      log.error("File unable to find app.properties in IDEA!");
    }
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }
}