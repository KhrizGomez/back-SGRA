package com.CLMTZ.Backend.service.report;

import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import com.CLMTZ.Backend.dto.report.AttendanceDetailRowDTO;
import com.CLMTZ.Backend.dto.report.ReportDataDTO;
import com.CLMTZ.Backend.dto.report.RequestDetailRowDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Component
public class PdfReportGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(0, 51, 102));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0, 51, 102));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Color HEADER_BG = new Color(0, 51, 102);
    private static final Font INST_NAME_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 51, 102));
    private static final Font INST_NAME_GREEN_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(27, 117, 5));

    public byte[] generateCoordinationDashboard(ReportDataDTO data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addInstitutionHeader(document, data.getInstitutionLogoUrl(), data.getInstitutionName(), INST_NAME_FONT);
        addTitle(document, "Reporte de Coordinacion", data.getPeriodName());

        addKpisSection(document, data.getKpis());
        addAttendanceSummarySection(document, data.getAsistencia());
        addSubjectSection(document, data.getSolicitudesPorMateria());
        addModalitySection(document, data.getModalidades());

        document.close();
        return out.toByteArray();
    }

    public byte[] generateAttendanceDetail(ReportDataDTO data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addInstitutionHeader(document, data.getInstitutionLogoUrl(), data.getInstitutionName(), INST_NAME_FONT);
        addTitle(document, "Reporte de Asistencia", data.getPeriodName());
        addAttendanceSummarySection(document, data.getAsistencia());

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Detalle de Asistencia", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        List<AttendanceDetailRowDTO> details = data.getAttendanceDetails();
        if (details != null && !details.isEmpty()) {
            PdfPTable table = new PdfPTable(new float[]{1f, 3f, 3f, 3f, 2f, 2f, 1.5f, 1.5f, 3f});
            table.setWidthPercentage(100);
            addTableHeaders(table, "ID", "Estudiante", "Asignatura", "Docente",
                            "Fecha", "Tipo", "Asistio", "Duracion", "Observaciones");

            for (AttendanceDetailRowDTO d : details) {
                addCell(table, d.getSessionId() != null ? String.valueOf(d.getSessionId()) : "");
                addCell(table, nullSafe(d.getStudentName()));
                addCell(table, nullSafe(d.getSubjectName()));
                addCell(table, nullSafe(d.getTeacherName()));
                addCell(table, nullSafe(d.getSessionDate()));
                addCell(table, nullSafe(d.getSessionType()));
                addCell(table, d.getAttended() != null ? (d.getAttended() ? "Si" : "No") : "N/A");
                addCell(table, nullSafe(d.getDuration()));
                addCell(table, nullSafe(d.getNotes()));
            }
            document.add(table);
        } else {
            document.add(new Paragraph("No se encontraron registros de asistencia.", CELL_FONT));
        }

        document.close();
        return out.toByteArray();
    }

    public byte[] generateRequestsDetail(ReportDataDTO data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addInstitutionHeader(document, data.getInstitutionLogoUrl(), data.getInstitutionName(), INST_NAME_FONT);
        addTitle(document, "Reporte de Solicitudes", data.getPeriodName());
        addSubjectSection(document, data.getSolicitudesPorMateria());

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Detalle de Solicitudes", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        List<RequestDetailRowDTO> details = data.getRequestDetails();
        if (details != null && !details.isEmpty()) {
            PdfPTable table = new PdfPTable(new float[]{1f, 2.5f, 3f, 3f, 3f, 2f, 2f, 3f});
            table.setWidthPercentage(100);
            addTableHeaders(table, "ID", "Fecha", "Estudiante", "Asignatura",
                            "Docente", "Tipo", "Estado", "Motivo");

            for (RequestDetailRowDTO d : details) {
                addCell(table, d.getRequestId() != null ? String.valueOf(d.getRequestId()) : "");
                addCell(table, nullSafe(d.getCreatedAt()));
                addCell(table, nullSafe(d.getStudentName()));
                addCell(table, nullSafe(d.getSubjectName()));
                addCell(table, nullSafe(d.getTeacherName()));
                addCell(table, nullSafe(d.getSessionType()));
                addCell(table, nullSafe(d.getStatusName()));
                addCell(table, nullSafe(d.getReason()));
            }
            document.add(table);
        } else {
            document.add(new Paragraph("No se encontraron solicitudes.", CELL_FONT));
        }

        document.close();
        return out.toByteArray();
    }

    private void addTitle(Document document, String title, String period) {
        document.add(new Paragraph(title, TITLE_FONT));
        document.add(new Paragraph("Periodo: " + nullSafe(period), VALUE_FONT));
        document.add(new Paragraph("\n"));
    }

    private void addKpisSection(Document document, CoordinationDashboardKpisDTO kpis) {
        document.add(new Paragraph("Indicadores Generales", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        if (kpis != null) {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(50);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addTableHeaders(table, "Indicador", "Valor");
            addCell(table, "Total Solicitudes");
            addCell(table, String.valueOf(kpis.getTotalSolicitudes()));
            addCell(table, "Pendientes");
            addCell(table, String.valueOf(kpis.getPendientes()));
            addCell(table, "Gestionadas");
            addCell(table, String.valueOf(kpis.getGestionadas()));
            document.add(table);
        }
        document.add(new Paragraph("\n"));
    }

    private void addAttendanceSummarySection(Document document, CoordinationDashboardAsistenciaDTO asistencia) {
        document.add(new Paragraph("Resumen de Asistencia", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        if (asistencia != null) {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(50);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addTableHeaders(table, "Indicador", "Valor");
            addCell(table, "Total Sesiones Registradas");
            addCell(table, String.valueOf(asistencia.getTotalSesionesRegistradas()));
            addCell(table, "Total Asistencias");
            addCell(table, String.valueOf(asistencia.getTotalAsistencias()));
            addCell(table, "Total Inasistencias");
            addCell(table, String.valueOf(asistencia.getTotalInasistencias()));
            addCell(table, "Porcentaje Asistencia");
            addCell(table, asistencia.getPorcentajeAsistencia() + "%");
            addCell(table, "Tasa Inasistencia");
            addCell(table, asistencia.getTasaInasistencia() + "%");
            document.add(table);
        }
        document.add(new Paragraph("\n"));
    }

    private void addSubjectSection(Document document, List<CoordinationDashboardSolicitudesMateriaDTO> materias) {
        document.add(new Paragraph("Solicitudes por Materia", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        if (materias != null && !materias.isEmpty()) {
            PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 1.5f, 1.5f});
            table.setWidthPercentage(70);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addTableHeaders(table, "Asignatura", "Total", "Pendientes", "Gestionadas");

            for (CoordinationDashboardSolicitudesMateriaDTO m : materias) {
                addCell(table, nullSafe(m.getAsignatura()));
                addCell(table, String.valueOf(m.getTotalMateria()));
                addCell(table, String.valueOf(m.getPendientes()));
                addCell(table, String.valueOf(m.getGestionadas()));
            }
            document.add(table);
        }
        document.add(new Paragraph("\n"));
    }

    private void addModalitySection(Document document, List<CoordinationDashboardModalidadDTO> modalidades) {
        document.add(new Paragraph("Distribucion por Modalidad", SUBTITLE_FONT));
        document.add(new Paragraph("\n"));

        if (modalidades != null && !modalidades.isEmpty()) {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(40);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addTableHeaders(table, "Modalidad", "Total");

            for (CoordinationDashboardModalidadDTO m : modalidades) {
                addCell(table, nullSafe(m.getModalidad()));
                addCell(table, String.valueOf(m.getTotal()));
            }
            document.add(table);
        }
        document.add(new Paragraph("\n"));
    }

    private void addTableHeaders(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Agrega un encabezado institucional al documento PDF con el logo y nombre de la institución.
     * El logo se descarga desde la URL desencriptada y se muestra a la izquierda del nombre.
     */
    private void addInstitutionHeader(Document document, String logoUrl, String institutionName, Font nameFont) {
        if ((logoUrl == null || logoUrl.isBlank()) && (institutionName == null || institutionName.isBlank())) {
            return;
        }
        try {
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 4f});
            headerTable.setSpacingAfter(10);

            // Celda del logo
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            if (logoUrl != null && !logoUrl.isBlank()) {
                try {
                    Image logo = Image.getInstance(new URL(logoUrl));
                    logo.scaleToFit(80, 80);
                    logoCell.addElement(logo);
                } catch (Exception e) {
                    logoCell.addElement(new Phrase(""));
                }
            } else {
                logoCell.addElement(new Phrase(""));
            }
            headerTable.addCell(logoCell);

            // Celda del nombre de la institución
            PdfPCell nameCell = new PdfPCell(new Phrase(nullSafe(institutionName), nameFont));
            nameCell.setBorder(PdfPCell.NO_BORDER);
            nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.setPaddingLeft(10);
            headerTable.addCell(nameCell);

            document.add(headerTable);
        } catch (Exception e) {
            // Si falla el encabezado institucional, el reporte se genera sin él
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  TABLA SIMPLE — replica la vista previa del frontend
    // ──────────────────────────────────────────────────────────────────────────

    private static final Font   PDF_TITLE_FONT  = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(27, 117, 5));
    private static final Font   PDF_DATE_FONT   = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(108, 117, 125));
    private static final Font   PDF_HDR_FONT    = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font   PDF_CELL_FONT   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Color  PDF_HDR_BG      = new Color(27, 117, 5);
    private static final Color  PDF_ALT_BG      = new Color(245, 250, 245);

    /**
     * Genera un PDF de una sola tabla con el mismo diseño que el frontend produce
     * con jsPDF + autoTable (colores verdes, sin gráficos).
     */
    public byte[] generateSimpleTable(List<java.util.Map<String, Object>> rows,
                                      List<String> keys,
                                      List<String> headers,
                                      String title,
                                      String periodName,
                                      String institutionName,
                                      String institutionLogoUrl) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 36, 28);
        PdfWriter.getInstance(document, out);
        document.open();

        // ── Encabezado institucional ──────────────────────────────────────────
        addInstitutionHeader(document, institutionLogoUrl, institutionName, INST_NAME_GREEN_FONT);

        // ── Título ───────────────────────────────────────────────────────────
        Paragraph titlePar = new Paragraph("Reporte – " + title, PDF_TITLE_FONT);
        titlePar.setSpacingAfter(4);
        document.add(titlePar);

        // ── Subtítulo: fecha y período ────────────────────────────────────────
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = String.format("%02d/%02d/%04d",
                today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        Paragraph datePar = new Paragraph("Generado el " + dateStr + "   |   Período: " + periodName, PDF_DATE_FONT);
        datePar.setSpacingAfter(8);
        document.add(datePar);

        // ── Línea separadora en verde (tabla auxiliar de 1 celda) ─────────────
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingAfter(10);
        PdfPCell sepCell = new PdfPCell(new Phrase(""));
        sepCell.setBorderColorBottom(PDF_HDR_BG);
        sepCell.setBorderWidthBottom(1.5f);
        sepCell.setBorderWidthTop(0);
        sepCell.setBorderWidthLeft(0);
        sepCell.setBorderWidthRight(0);
        sepCell.setPaddingBottom(0);
        separator.addCell(sepCell);
        document.add(separator);

        // ── Tabla de datos ────────────────────────────────────────────────────
        if (rows == null || rows.isEmpty()) {
            document.add(new Paragraph("No se encontraron registros.", PDF_CELL_FONT));
        } else {
            // Widths iguales para todas las columnas
            float[] widths = new float[headers.size()];
            java.util.Arrays.fill(widths, 1f);
            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);

            // Encabezados
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, PDF_HDR_FONT));
                cell.setBackgroundColor(PDF_HDR_BG);
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }

            // Datos
            boolean alt = false;
            for (java.util.Map<String, Object> row : rows) {
                for (String key : keys) {
                    Object val = row.get(key);
                    String text = val != null ? val.toString() : "";
                    PdfPCell cell = new PdfPCell(new Phrase(text, PDF_CELL_FONT));
                    cell.setPadding(4);
                    if (alt) cell.setBackgroundColor(PDF_ALT_BG);
                    table.addCell(cell);
                }
                alt = !alt;
            }
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }
}

