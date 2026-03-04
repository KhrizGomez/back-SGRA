package com.CLMTZ.Backend.service.report.impl;

import com.CLMTZ.Backend.dto.report.ReportDataDTO;
import com.CLMTZ.Backend.repository.report.ReportRepository;
import com.CLMTZ.Backend.service.report.ExcelReportGenerator;
import com.CLMTZ.Backend.service.report.PdfReportGenerator;
import com.CLMTZ.Backend.service.report.ReportService;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ExcelReportGenerator excelGenerator;
    private final PdfReportGenerator pdfGenerator;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ExcelReportGenerator excelGenerator,
                             PdfReportGenerator pdfGenerator) {
        this.reportRepository = reportRepository;
        this.excelGenerator = excelGenerator;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public byte[] generateReport(String reportType, String format) {
        ReportDataDTO data = getReportData(reportType);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generatePdf(reportType, data);
            } else {
                return generateExcel(reportType, data);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte: " + e.getMessage(), e);
        }
    }

    @Override
    public ReportDataDTO getReportData(String reportType) {
        Integer periodId = reportRepository.getActivePeriodId();
        String periodName = reportRepository.getActivePeriodName();

        ReportDataDTO data = new ReportDataDTO();
        data.setPeriodName(periodName);

        switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD":
                data.setReportTitle("Reporte de Coordinacion");
                data.setKpis(reportRepository.getKpis(periodId));
                data.setAsistencia(reportRepository.getAsistencia(periodId));
                data.setSolicitudesPorMateria(reportRepository.getSolicitudesPorMateria(periodId));
                data.setModalidades(reportRepository.getModalidades(periodId));
                break;

            case "ATTENDANCE_DETAIL":
                data.setReportTitle("Reporte de Asistencia");
                data.setAsistencia(reportRepository.getAsistencia(periodId));
                data.setAttendanceDetails(reportRepository.getAttendanceDetails(periodId));
                break;

            case "REQUESTS_DETAIL":
                data.setReportTitle("Reporte de Solicitudes");
                data.setSolicitudesPorMateria(reportRepository.getSolicitudesPorMateria(periodId));
                data.setRequestDetails(reportRepository.getRequestDetails(periodId));
                break;

            default:
                throw new IllegalArgumentException("Tipo de reporte no valido: " + reportType);
        }

        return data;
    }

    private byte[] generateExcel(String reportType, ReportDataDTO data) throws IOException {
        return switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD" -> excelGenerator.generateCoordinationDashboard(data);
            case "ATTENDANCE_DETAIL" -> excelGenerator.generateAttendanceDetail(data);
            case "REQUESTS_DETAIL" -> excelGenerator.generateRequestsDetail(data);
            default -> throw new IllegalArgumentException("Tipo de reporte no valido: " + reportType);
        };
    }

    private byte[] generatePdf(String reportType, ReportDataDTO data) throws IOException {
        return switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD" -> pdfGenerator.generateCoordinationDashboard(data);
            case "ATTENDANCE_DETAIL" -> pdfGenerator.generateAttendanceDetail(data);
            case "REQUESTS_DETAIL" -> pdfGenerator.generateRequestsDetail(data);
            default -> throw new IllegalArgumentException("Tipo de reporte no valido: " + reportType);
        };
    }
}
