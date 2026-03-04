package com.CLMTZ.Backend.controller.report;

import com.CLMTZ.Backend.service.report.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadReport(
            @RequestParam("type") String reportType,
            @RequestParam(value = "format", defaultValue = "EXCEL") String format) {
        try {
            byte[] reportBytes = reportService.generateReport(reportType, format);

            String filename;
            MediaType mediaType;

            if ("PDF".equalsIgnoreCase(format)) {
                filename = buildFilename(reportType, "pdf");
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                filename = buildFilename(reportType, "xlsx");
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .contentLength(reportBytes.length)
                    .body(reportBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al generar el reporte: " + e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<?> getAvailableReportTypes() {
        return ResponseEntity.ok(Map.of(
                "types", java.util.List.of(
                        Map.of("id", "COORDINATION_DASHBOARD",
                               "name", "Dashboard de Coordinacion",
                               "description", "KPIs, asistencia, solicitudes por materia y modalidad"),
                        Map.of("id", "ATTENDANCE_DETAIL",
                               "name", "Asistencia Detallada",
                               "description", "Resumen y detalle de asistencia por sesion"),
                        Map.of("id", "REQUESTS_DETAIL",
                               "name", "Solicitudes de Reforzamiento",
                               "description", "Resumen y detalle de todas las solicitudes")
                ),
                "formats", java.util.List.of("EXCEL", "PDF")
        ));
    }

    private String buildFilename(String reportType, String extension) {
        String base = switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD" -> "reporte_coordinacion";
            case "ATTENDANCE_DETAIL" -> "reporte_asistencia";
            case "REQUESTS_DETAIL" -> "reporte_solicitudes";
            default -> "reporte";
        };
        return base + "." + extension;
    }
}
