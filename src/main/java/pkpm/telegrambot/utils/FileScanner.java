package pkpm.telegrambot.utils;

import java.io.File;
import java.util.Date;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class FileScanner {

  private final File file;
  private Date fileDate;

  public FileScanner(String fileName) {
    this.file = new File(fileName);
    this.fileDate = new Date(file.lastModified());
    log.info("File exist : {}", file.exists());
  }

  public Date getCurrentFileDate() {
    return new Date(file.lastModified());
  }

  private boolean checkFileUpdateStatus() {
    if (fileDate.before(getCurrentFileDate())) {
      this.fileDate = getCurrentFileDate();
      return true;
    } else {
      return false;
    }
  }

  public void scanner() {
    while (true) {
      boolean flag = checkFileUpdateStatus();
      if (flag) {
        break;
      }
      pause(Integer.parseInt(System.getenv("INTERVAL_TIME")));
    }
  }

  private void pause(long seconds) {
    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

}
