package com.CLMTZ.Backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {
    private String reportType;  // COORDINATION_DASHBOARD, ATTENDANCE_DETAIL, REQUESTS_DETAIL
    private String format;      // EXCEL, PDF
}
