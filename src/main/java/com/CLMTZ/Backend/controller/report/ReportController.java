package com.CLMTZ.Backend.controller.report;

import com.CLMTZ.Backend.service.report.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    /** Tipos de reporte que usan la nueva ruta simple (tabla única). */
    private static final Set<String> SIMPLE_TYPES = Set.of(
            "BY_SUBJECT", "BY_TEACHER", "BY_PARALLEL", "BY_GRADE", "BY_STUDENT_REQUESTS",
            "BY_SECTION_AND_GRADE");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ── Descarga ──────────────────────────────────────────────────────────────
    @GetMapping("/download")
    public ResponseEntity<?> downloadReport(
            @RequestParam("type")                                String reportType,
            @RequestParam(value = "format",  defaultValue = "EXCEL") String format,
            @RequestParam(value = "columns", required = false)   String columns,
            @RequestParam(value = "dateFrom",required = false)   String dateFrom,
            @RequestParam(value = "dateTo",  required = false)   String dateTo,
            @RequestParam(value = "period",  required = false)   String period) {
        try {
            byte[] reportBytes;
            String upperType = reportType.toUpperCase();

            if (SIMPLE_TYPES.contains(upperType)) {
                List<String> colList = (columns != null && !columns.isBlank())
                        ? Arrays.asList(columns.split(","))
                        : null;
                reportBytes = reportService.generateSimpleReport(upperType, format, colList, dateFrom, dateTo, period);
            } else {
                reportBytes = reportService.generateReport(upperType, format);
            }

            String filename;
            MediaType mediaType;

            if ("PDF".equalsIgnoreCase(format)) {
                filename  = buildFilename(reportType, "pdf");
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                filename  = buildFilename(reportType, "xlsx");
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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

    // ── Vista previa (datos para el frontend) ─────────────────────────────────
    @GetMapping("/preview")
    public ResponseEntity<?> getPreview(
            @RequestParam("type")                                String reportType,
            @RequestParam(value = "dateFrom", required = false)  String dateFrom,
            @RequestParam(value = "dateTo",   required = false)  String dateTo,
            @RequestParam(value = "period",   required = false)  String period) {
        try {
            List<Map<String, Object>> rows =
                    reportService.getPreviewRows(reportType.toUpperCase(), dateFrom, dateTo, period);
            return ResponseEntity.ok(rows);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al obtener la vista previa: " + e.getMessage()));
        }
    }

    // ── Tipos disponibles ─────────────────────────────────────────────────────
    @GetMapping("/types")
    public ResponseEntity<?> getAvailableReportTypes() {
        return ResponseEntity.ok(Map.of(
                "types", java.util.List.of(
                        Map.of("id", "BY_SUBJECT",
                               "name", "Por Materia",
                               "description", "Solicitudes de refuerzo agrupadas por asignatura"),
                        Map.of("id", "BY_TEACHER",
                               "name", "Por Docente",
                               "description", "Carga de sesiones de refuerzo por cada docente"),
                        Map.of("id", "BY_PARALLEL",
                               "name", "Por Paralelo",
                               "description", "Distribucion de solicitudes por paralelo"),
                        Map.of("id", "BY_GRADE",
                               "name", "Por Curso",
                               "description", "Solicitudes de refuerzo por nivel/semestre"),
                        Map.of("id", "BY_STUDENT_REQUESTS",
                               "name", "Por Estudiante",
                               "description", "Solicitudes de refuerzo por estudiante"),
                        Map.of("id", "BY_SECTION_AND_GRADE",
                               "name", "Por Paralelo y Curso",
                               "description", "Distribucion de solicitudes por paralelo y nivel (semestre)")
                ),
                "formats", java.util.List.of("EXCEL", "PDF")
        ));
    }

    private String buildFilename(String reportType, String extension) {
        String base = switch (reportType.toUpperCase()) {
            case "BY_SUBJECT"          -> "reporte_por_materia";
            case "BY_TEACHER"          -> "reporte_por_docente";
            case "BY_PARALLEL"         -> "reporte_por_paralelo";
            case "BY_GRADE"            -> "reporte_por_curso";
            case "BY_STUDENT_REQUESTS" -> "reporte_por_estudiante";
            case "BY_SECTION_AND_GRADE"  -> "reporte_por_paralelo_y_curso";
            case "COORDINATION_DASHBOARD" -> "reporte_coordinacion";
            case "ATTENDANCE_DETAIL"      -> "reporte_asistencia";
            case "REQUESTS_DETAIL"        -> "reporte_solicitudes";
            default -> "reporte";
        };
        return base + "." + extension;
    }
}

