package pkpm.telegrambot.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertiesLoader {

  private final Properties properties = new Properties();

  public PropertiesLoader() {
    String fileProperties = "app.properties";
    String path = "src/main/resources/";
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileProperties)) {
      if (input == null) {
        loadFromProject(path, fileProperties);
      } else {
        properties.load(input);
      }
    } catch (IOException ex) {
      log.warn("Sorry, unable to find app.properties!\n{}", ex.getMessage());
    }
  }

  private void loadFromProject(String path, String fileProperties) {
    try (FileInputStream inputStream = new FileInputStream(path + fileProperties)) {
      properties.load(inputStream);
    } catch (IOException exception) {
      log.error("File unable to find app.properties in IDEA!");
    }
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }
}