package com.CLMTZ.Backend.service.report;

import com.CLMTZ.Backend.dto.report.ReportDataDTO;

import java.util.List;
import java.util.Map;

public interface ReportService {

    /** Generación de reportes con el formato antiguo (multi-hoja). */
    byte[] generateReport(String reportType, String format);

    ReportDataDTO getReportData(String reportType);

    /**
     * Generación de reporte simple (tabla única) que replica la vista previa del frontend.
     *
     * @param reportType tipo de reporte (BY_SUBJECT, BY_TEACHER, BY_PARALLEL, BY_GRADE, BY_STUDENT_REQUESTS)
     * @param format     EXCEL o PDF
     * @param columns    lista de claves de columnas visibles (puede ser null → todas)
     * @param dateFrom   filtro de fecha inicio (puede ser null)
     * @param dateTo     filtro de fecha fin (puede ser null)
     */
    /**
     * @param period nombre del período a consultar; si es null usa el período activo
     */
    byte[] generateSimpleReport(String reportType, String format,
                                List<String> columns,
                                String dateFrom, String dateTo,
                                String period);

    /**
     * Devuelve las filas de datos para la vista previa del frontend.
     *
     * @param period nombre del período a consultar (ej. "SPA 2026"); si es null usa el período activo
     */
    List<Map<String, Object>> getPreviewRows(String reportType,
                                             String dateFrom, String dateTo,
                                             String period);
}
