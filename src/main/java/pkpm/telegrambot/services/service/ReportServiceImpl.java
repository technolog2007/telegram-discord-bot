package pkpm.telegrambot.services.service;

import java.util.List;
import java.util.Map;
import pkpm.company.automation.models.Employees;
import pkpm.company.automation.models.ReportEmployee;
import pkpm.company.automation.models.ReportGeneral;
import pkpm.company.automation.services.GraphExecutionReport;
import pkpm.company.automation.services.MakeSnapshot;

public class ReportServiceImpl implements ReportService {

  private final GraphExecutionReport executionReport;

  public ReportServiceImpl(GraphExecutionReport executionReport) {
    this.executionReport = executionReport;
  }

  @Override
  public String createGeneralReport(String graphName) {
    List<ReportGeneral> listOfResults = executionReport.getDateForGeneralReport(
        new MakeSnapshot(graphName).getBs());
    return executionReport.writeResultToString(listOfResults);
  }

  @Override
  public String createEmployeeReport(String graphName, String employeeName) {
    Map<Employees, List<ReportEmployee>> result = executionReport.getListOfEmployeesReports(
        new MakeSnapshot(graphName).getBs());
    return executionReport.writeResulForReportEmployeetToString(
        result.get(Employees.fromName(employeeName)));
  }
}
