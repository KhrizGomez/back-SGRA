package com.CLMTZ.Backend.service.report;

import com.CLMTZ.Backend.dto.report.ReportDataDTO;

public interface ReportService {

    byte[] generateReport(String reportType, String format);

    ReportDataDTO getReportData(String reportType);
}
