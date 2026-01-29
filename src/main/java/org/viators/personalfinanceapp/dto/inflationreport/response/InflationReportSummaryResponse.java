package org.viators.personalfinanceapp.dto.inflationreport.response;

import org.viators.personalfinanceapp.model.InflationReport;
import org.viators.personalfinanceapp.model.enums.ReportTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InflationReportSummaryResponse(
        ReportTypeEnum reportType,
        String categoryName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal inflationRate
) {
    public static InflationReportSummaryResponse from(InflationReport inflationReport) {
        return new InflationReportSummaryResponse(
                inflationReport.getReportType(),
                inflationReport.getCategory().getName(),
                inflationReport.getStartDate(),
                inflationReport.getEndDate(),
                inflationReport.getInflationRate()
        );
    }

    public static List<InflationReportSummaryResponse> listOfSummaries(List<InflationReport> inflationReports) {
        return inflationReports.stream()
                .map(InflationReportSummaryResponse::from)
                .toList();
    }
}