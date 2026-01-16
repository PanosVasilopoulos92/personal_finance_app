package org.viators.personal_finance_app.dto.inflationreport.response;

import org.viators.personal_finance_app.model.InflationReport;
import org.viators.personal_finance_app.model.enums.ReportTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InflationReportSummary(
        ReportTypeEnum reportType,
        String categoryName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal inflationRate
) {
    public static InflationReportSummary from(InflationReport inflationReport) {
        return new InflationReportSummary(
                inflationReport.getReportType(),
                inflationReport.getCategory().getName(),
                inflationReport.getStartDate(),
                inflationReport.getEndDate(),
                inflationReport.getInflationRate()
        );
    }

    public static List<InflationReportSummary> listOfSummaries(List<InflationReport> inflationReports) {
        return inflationReports.stream()
                .map(InflationReportSummary::from)
                .toList();
    }
}