package pkpm.telegrambot.services.service;

public interface ReportService {
  String createGeneralReport(String graphName);
  String createEmployeeReport(String graphName, String employeeName);
}
