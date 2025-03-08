package pkpm.echobot.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import pkpm.echobot.app.App;

@Slf4j
public class PropertiesLoader {

    private static final String PROPERTIES_FILE = "app.properties";
    private static final String PATH = "src/main/resources/";

    /**
     * The method loads data from the properties file into a buffer.
     * The method works when running the program from IDEA and external properties from Jar.
     *
     * @return - link to properties buffer
     */
    public Properties loadProperties() {

        Properties properties = new Properties();

        try {
            InputStream rootPath = App.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE);
            properties.load(rootPath);
            return properties;
        } catch (Exception e) {
            try (FileInputStream inputStream = new FileInputStream(PATH + PROPERTIES_FILE)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                log.error("Something went wrong with reading the properties!");
            }
            return properties;
        }
    }


}
