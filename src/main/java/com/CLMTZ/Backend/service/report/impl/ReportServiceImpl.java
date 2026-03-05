package com.CLMTZ.Backend.service.report.impl;

import com.CLMTZ.Backend.dto.report.ReportDataDTO;
import com.CLMTZ.Backend.repository.report.ReportRepository;
import com.CLMTZ.Backend.service.report.ExcelReportGenerator;
import com.CLMTZ.Backend.service.report.PdfReportGenerator;
import com.CLMTZ.Backend.service.report.ReportService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    // ── Etiquetas de columna (clave del frontend → etiqueta legible) ──────────
    private static final Map<String, String> COLUMN_LABELS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("asignatura",      "Materia");
        m.put("totalMateria",    "Total Solicitudes");
        m.put("pendientes",      "Pendientes");
        m.put("gestionadas",     "Gestionadas");
        m.put("docente",         "Docente");
        m.put("materia",         "Materia");
        m.put("sesiones",        "Sesiones Dictadas");
        m.put("estudiantes",     "Estudiantes Atendidos");
        m.put("paralelo",        "Paralelo");
        m.put("solicitudes",     "Solicitudes");
        m.put("asistencia",      "Asistencia (%)");
        m.put("inasistencia",    "Inasistencia (%)");
        m.put("curso",           "Curso");
        m.put("estudiante",      "Estudiante");
        m.put("materia_refuerzo","Materia con más refuerzo");
        COLUMN_LABELS = Collections.unmodifiableMap(m);
    }

    // ── Todas las claves por tipo de reporte (orden por defecto) ─────────────
    private static final Map<String, List<String>> DEFAULT_COLUMNS;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("BY_SUBJECT",          List.of("asignatura", "totalMateria", "pendientes", "gestionadas"));
        m.put("BY_TEACHER",          List.of("docente", "materia", "sesiones", "estudiantes"));
        m.put("BY_PARALLEL",         List.of("paralelo", "materia", "solicitudes", "asistencia", "inasistencia"));
        m.put("BY_GRADE",            List.of("curso", "solicitudes", "asistencia", "inasistencia"));
        m.put("BY_STUDENT_REQUESTS", List.of("estudiante", "solicitudes", "materia_refuerzo", "asistencia"));
        DEFAULT_COLUMNS = Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> REPORT_TITLES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("BY_SUBJECT",          "Solicitudes por Materia");
        m.put("BY_TEACHER",          "Sesiones por Docente");
        m.put("BY_PARALLEL",         "Solicitudes por Paralelo");
        m.put("BY_GRADE",            "Solicitudes por Curso");
        m.put("BY_STUDENT_REQUESTS", "Solicitudes por Estudiante");
        REPORT_TITLES = Collections.unmodifiableMap(m);
    }

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

    // ── Reporte simple (replica la vista previa del frontend) ─────────────────

    @Override
    public List<Map<String, Object>> getPreviewRows(String reportType, String dateFrom, String dateTo) {
        Integer periodId = reportRepository.getActivePeriodId();
        return fetchRows(reportType.toUpperCase(), periodId, dateFrom, dateTo);
    }

    @Override
    public byte[] generateSimpleReport(String reportType,
                                       String format,
                                       List<String> columns,
                                       String dateFrom,
                                       String dateTo) {
        String type = reportType.toUpperCase();

        if (!DEFAULT_COLUMNS.containsKey(type)) {
            throw new IllegalArgumentException("Tipo de reporte no válido: " + reportType);
        }

        Integer periodId = reportRepository.getActivePeriodId();
        String periodName = reportRepository.getActivePeriodName();

        List<Map<String, Object>> rows = fetchRows(type, periodId, dateFrom, dateTo);

        // Determinar columnas visibles respetando el orden por defecto
        List<String> keys = resolveColumns(type, columns);
        List<String> headers = keys.stream()
                .map(k -> COLUMN_LABELS.getOrDefault(k, k))
                .toList();

        String title = REPORT_TITLES.getOrDefault(type, "Reporte");

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return pdfGenerator.generateSimpleTable(rows, keys, headers, title, periodName);
            } else {
                return excelGenerator.generateSimpleTable(rows, keys, headers, title, periodName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchRows(String type, Integer periodId,
                                                String dateFrom, String dateTo) {
        return switch (type) {
            case "BY_SUBJECT"          -> reportRepository.getPreviewBySubject(periodId, dateFrom, dateTo);
            case "BY_TEACHER"          -> reportRepository.getPreviewByTeacher(periodId, dateFrom, dateTo);
            case "BY_PARALLEL"         -> reportRepository.getPreviewByParallel(periodId, dateFrom, dateTo);
            case "BY_GRADE"            -> reportRepository.getPreviewByGrade(periodId, dateFrom, dateTo);
            case "BY_STUDENT_REQUESTS" -> reportRepository.getPreviewByStudent(periodId, dateFrom, dateTo);
            default -> Collections.emptyList();
        };
    }

    private List<String> resolveColumns(String type, List<String> requested) {
        List<String> defaults = DEFAULT_COLUMNS.getOrDefault(type, List.of());
        if (requested == null || requested.isEmpty()) return defaults;
        // Mantener el orden del defecto, filtrando sólo las pedidas
        return defaults.stream().filter(requested::contains).toList();
    }

    private byte[] generateExcel(String reportType, ReportDataDTO data) throws IOException {
        return switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD" -> excelGenerator.generateCoordinationDashboard(data);
            case "ATTENDANCE_DETAIL"      -> excelGenerator.generateAttendanceDetail(data);
            case "REQUESTS_DETAIL"        -> excelGenerator.generateRequestsDetail(data);
            default -> throw new IllegalArgumentException("Tipo de reporte no valido: " + reportType);
        };
    }

    private byte[] generatePdf(String reportType, ReportDataDTO data) throws IOException {
        return switch (reportType.toUpperCase()) {
            case "COORDINATION_DASHBOARD" -> pdfGenerator.generateCoordinationDashboard(data);
            case "ATTENDANCE_DETAIL"      -> pdfGenerator.generateAttendanceDetail(data);
            case "REQUESTS_DETAIL"        -> pdfGenerator.generateRequestsDetail(data);
            default -> throw new IllegalArgumentException("Tipo de reporte no valido: " + reportType);
        };
    }
}

