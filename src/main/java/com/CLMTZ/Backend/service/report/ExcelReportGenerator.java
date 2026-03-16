package com.CLMTZ.Backend.service.report;

import com.CLMTZ.Backend.dto.report.ReportDataDTO;
import com.CLMTZ.Backend.dto.report.AttendanceDetailRowDTO;
import com.CLMTZ.Backend.dto.report.RequestDetailRowDTO;
import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Component
public class ExcelReportGenerator {

    // ──────────────────────────────────────────────────────────────────────────
    //  PUNTOS DE ENTRADA PÚBLICOS
    // ──────────────────────────────────────────────────────────────────────────

    public byte[] generateCoordinationDashboard(ReportDataDTO data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            XSSFSheet firstSheet = wb.createSheet("KPIs Generales");
            int ri = addInstitutionHeaderToSheet(wb, firstSheet, 0, 2, data.getInstitutionLogoUrl(), data.getInstitutionName());
            createKpisSheetContent(firstSheet, ri, data, s);
            createAttendanceSheet(wb, data, s);
            createSubjectSheet(wb, data, s);
            createModalitySheet(wb, data, s);
            return toBytes(wb);
        }
    }

    public byte[] generateAttendanceDetail(ReportDataDTO data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            XSSFSheet firstSheet = wb.createSheet("Resumen Asistencia");
            int ri = addInstitutionHeaderToSheet(wb, firstSheet, 0, 2, data.getInstitutionLogoUrl(), data.getInstitutionName());
            createAttendanceSheetContent(firstSheet, ri, data, s);
            createAttendanceDetailSheet(wb, data, s);
            return toBytes(wb);
        }
    }

    public byte[] generateRequestsDetail(ReportDataDTO data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            XSSFSheet firstSheet = wb.createSheet("Solicitudes por Materia");
            int ri = addInstitutionHeaderToSheet(wb, firstSheet, 0, 3, data.getInstitutionLogoUrl(), data.getInstitutionName());
            createSubjectSheetContent(firstSheet, ri, data, s);
            createRequestsDetailSheet(wb, data, s);
            return toBytes(wb);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTORES DE HOJAS
    // ──────────────────────────────────────────────────────────────────────────

    private void createKpisSheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("KPIs Generales");
        createKpisSheetContent(sheet, 0, data, s);
    }

    private void createKpisSheetContent(XSSFSheet sheet, int ri, ReportDataDTO data, Styles s) {

        ri = writeMergedTitle(sheet, ri,
                "Reporte de Coordinación - SGRA  |  " + data.getPeriodName(), s.title, 2);
        ri++; // fila en blanco

        String[] headers = {"Indicador", "Valor"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        int dataStart = ri;
        CoordinationDashboardKpisDTO kpis = data.getKpis();
        if (kpis != null) {
            writeKpiRow(sheet, ri++, "Total Solicitudes", kpis.getTotalSolicitudes(), s);
            writeKpiRow(sheet, ri++, "Pendientes",        kpis.getPendientes(),        s);
            writeKpiRow(sheet, ri++, "Gestionadas",       kpis.getGestionadas(),       s);
        }
        int dataEnd = ri - 1;

        autoSize(sheet, headers.length);

        if (dataEnd >= dataStart) {
            addBarChart(sheet, "KPIs Generales", dataStart, dataEnd, 0, 1,
                    0, ri + 2, 7, ri + 18);
        }
    }

    private void createAttendanceSheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("Resumen Asistencia");
        createAttendanceSheetContent(sheet, 0, data, s);
    }

    private void createAttendanceSheetContent(XSSFSheet sheet, int ri, ReportDataDTO data, Styles s) {

        ri = writeMergedTitle(sheet, ri,
                "Resumen de Asistencia - " + data.getPeriodName(), s.title, 2);
        ri++;

        String[] headers = {"Indicador", "Valor"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        int dataStart = ri;
        int numericEnd = ri - 1;
        CoordinationDashboardAsistenciaDTO a = data.getAsistencia();
        if (a != null) {
            writeKpiRow(sheet, ri++, "Total Sesiones Registradas", a.getTotalSesionesRegistradas(), s);
            writeKpiRow(sheet, ri++, "Total Asistencias",          a.getTotalAsistencias(),          s);
            writeKpiRow(sheet, ri++, "Total Inasistencias",        a.getTotalInasistencias(),        s);
            numericEnd = ri - 1;
            writeKpiRowStr(sheet, ri++, "% Asistencia",    a.getPorcentajeAsistencia() + " %", s);
            writeKpiRowStr(sheet, ri++, "Tasa Inasistencia", a.getTasaInasistencia() + " %",   s);
        }

        autoSize(sheet, headers.length);

        // Solo las 3 filas numéricas alimentan la gráfica
        if (numericEnd >= dataStart) {
            addBarChart(sheet, "Sesiones", dataStart, numericEnd, 0, 1,
                    0, ri + 2, 7, ri + 18);
        }
    }

    private void createSubjectSheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("Solicitudes por Materia");
        createSubjectSheetContent(sheet, 0, data, s);
    }

    private void createSubjectSheetContent(XSSFSheet sheet, int ri, ReportDataDTO data, Styles s) {

        ri = writeMergedTitle(sheet, ri,
                "Solicitudes por Materia - " + data.getPeriodName(), s.title, 3);
        ri++;

        String[] headers = {"Asignatura", "Total", "Pendientes", "Gestionadas"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        int dataStart = ri;
        List<CoordinationDashboardSolicitudesMateriaDTO> list = data.getSolicitudesPorMateria();
        if (list != null) {
            for (CoordinationDashboardSolicitudesMateriaDTO m : list) {
                Row row = sheet.createRow(ri);
                CellStyle cs = rowStyle(ri, s);
                applyStr(row.createCell(0), nz(m.getAsignatura()),     cs);
                applyNum(row.createCell(1), m.getTotalMateria(),        cs);
                applyNum(row.createCell(2), m.getPendientes(),          cs);
                applyNum(row.createCell(3), m.getGestionadas(),         cs);
                ri++;
            }
        }
        int dataEnd = ri - 1;

        autoSize(sheet, headers.length);

        if (dataEnd >= dataStart) {
            addBarChart(sheet, "Total por Materia", dataStart, dataEnd, 0, 1,
                    0, ri + 2, 8, ri + 20);
        }
    }

    private void createModalitySheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("Modalidades");
        int ri = 0;

        ri = writeMergedTitle(sheet, ri,
                "Distribución por Modalidad - " + data.getPeriodName(), s.title, 2);
        ri++;

        String[] headers = {"Modalidad", "Total"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        int dataStart = ri;
        List<CoordinationDashboardModalidadDTO> list = data.getModalidades();
        if (list != null) {
            for (CoordinationDashboardModalidadDTO m : list) {
                Row row = sheet.createRow(ri);
                CellStyle cs = rowStyle(ri, s);
                applyStr(row.createCell(0), nz(m.getModalidad()), cs);
                applyNum(row.createCell(1), m.getTotal(),         cs);
                ri++;
            }
        }
        int dataEnd = ri - 1;

        autoSize(sheet, headers.length);

        if (dataEnd >= dataStart) {
            addPieChart(sheet, "Distribución por Modalidad", dataStart, dataEnd, 0, 1,
                    0, ri + 2, 8, ri + 18);
        }
    }

    private void createAttendanceDetailSheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("Detalle Asistencia");
        int ri = 0;

        ri = writeMergedTitle(sheet, ri,
                "Detalle de Asistencia - " + data.getPeriodName(), s.title, 8);
        ri++;

        String[] headers = {"ID Sesión", "Estudiante", "Asignatura", "Docente",
                            "Fecha", "Tipo Sesión", "Asistió", "Duración", "Observaciones"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        List<AttendanceDetailRowDTO> details = data.getAttendanceDetails();
        if (details != null) {
            for (AttendanceDetailRowDTO d : details) {
                Row row = sheet.createRow(ri);
                CellStyle cs = rowStyle(ri, s);
                applyNum(row.createCell(0), d.getSessionId(),                                    cs);
                applyStr(row.createCell(1), nz(d.getStudentName()),                              cs);
                applyStr(row.createCell(2), nz(d.getSubjectName()),                              cs);
                applyStr(row.createCell(3), nz(d.getTeacherName()),                              cs);
                applyStr(row.createCell(4), nz(d.getSessionDate()),                              cs);
                applyStr(row.createCell(5), nz(d.getSessionType()),                              cs);
                applyStr(row.createCell(6),
                        d.getAttended() != null ? (d.getAttended() ? "Sí" : "No") : "N/A",     cs);
                applyStr(row.createCell(7), nz(d.getDuration()),                                 cs);
                applyStr(row.createCell(8), nz(d.getNotes()),                                    cs);
                ri++;
            }
        }
        autoSize(sheet, headers.length);
    }

    private void createRequestsDetailSheet(XSSFWorkbook wb, ReportDataDTO data, Styles s) {
        XSSFSheet sheet = wb.createSheet("Detalle Solicitudes");
        int ri = 0;

        ri = writeMergedTitle(sheet, ri,
                "Detalle de Solicitudes - " + data.getPeriodName(), s.title, 7);
        ri++;

        String[] headers = {"ID", "Fecha Creación", "Estudiante", "Asignatura",
                            "Docente", "Tipo Sesión", "Estado", "Motivo"};
        writeHeaderRow(sheet, ri++, headers, s.header);

        List<RequestDetailRowDTO> details = data.getRequestDetails();
        if (details != null) {
            for (RequestDetailRowDTO d : details) {
                Row row = sheet.createRow(ri);
                CellStyle cs = rowStyle(ri, s);
                applyNum(row.createCell(0), d.getRequestId(),      cs);
                applyStr(row.createCell(1), nz(d.getCreatedAt()),   cs);
                applyStr(row.createCell(2), nz(d.getStudentName()), cs);
                applyStr(row.createCell(3), nz(d.getSubjectName()), cs);
                applyStr(row.createCell(4), nz(d.getTeacherName()), cs);
                applyStr(row.createCell(5), nz(d.getSessionType()), cs);
                applyStr(row.createCell(6), nz(d.getStatusName()),  cs);
                applyStr(row.createCell(7), nz(d.getReason()),      cs);
                ri++;
            }
        }
        autoSize(sheet, headers.length);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GRÁFICAS (XDDF)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Inserta una gráfica de barras verticales usando los datos ya escritos en la hoja.
     * catCol  = columna de categorías (texto)
     * valCol  = columna de valores (numérico)
     * Ancla: celdas (ancC1, ancR1) → (ancC2, ancR2)
     */
    private void addBarChart(XSSFSheet sheet, String title,
                             int dataStart, int dataEnd,
                             int catCol, int valCol,
                             int ancC1, int ancR1, int ancC2, int ancR2) {
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0,
                    ancC1, ancR1, ancC2, ancR2);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(title);
            chart.setTitleOverlay(false);

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis    yAxis = chart.createValueAxis(AxisPosition.LEFT);
            yAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String>         cats = XDDFDataSourcesFactory.fromStringCellRange(
                    sheet, new CellRangeAddress(dataStart, dataEnd, catCol, catCol));
            XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(dataStart, dataEnd, valCol, valCol));

            XDDFBarChartData barData =
                    (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
            barData.setBarDirection(BarDirection.COL);
            XDDFBarChartData.Series series =
                    (XDDFBarChartData.Series) barData.addSeries(cats, vals);
            series.setTitle(title, null);
            chart.plot(barData);

        } catch (Exception ignored) {
            // Si la gráfica falla, el archivo Excel igual se genera correctamente
        }
    }

    /**
     * Inserta una gráfica de pastel.
     */
    private void addPieChart(XSSFSheet sheet, String title,
                             int dataStart, int dataEnd,
                             int catCol, int valCol,
                             int ancC1, int ancR1, int ancC2, int ancR2) {
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0,
                    ancC1, ancR1, ancC2, ancR2);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(title);
            chart.setTitleOverlay(false);

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.RIGHT);

            XDDFDataSource<String>         cats = XDDFDataSourcesFactory.fromStringCellRange(
                    sheet, new CellRangeAddress(dataStart, dataEnd, catCol, catCol));
            XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(dataStart, dataEnd, valCol, valCol));

            XDDFPieChartData pieData =
                    (XDDFPieChartData) chart.createData(ChartTypes.PIE, null, null);
            pieData.setVaryColors(true);
            XDDFPieChartData.Series series =
                    (XDDFPieChartData.Series) pieData.addSeries(cats, vals);
            series.setTitle(title, null);
            chart.plot(pieData);

        } catch (Exception ignored) {
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UTILIDADES DE ESCRITURA
    // ──────────────────────────────────────────────────────────────────────────

    /** Fila de título fusionada en columnas 0..lastCol con altura 30pt. */
    private int writeMergedTitle(XSSFSheet sheet, int ri,
                                 String text, CellStyle style, int lastCol) {
        Row row = sheet.createRow(ri);
        row.setHeightInPoints(30);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        if (lastCol > 0) {
            sheet.addMergedRegion(new CellRangeAddress(ri, ri, 0, lastCol));
        }
        return ri + 1;
    }

    private void writeHeaderRow(XSSFSheet sheet, int ri,
                                String[] headers, CellStyle style) {
        Row row = sheet.createRow(ri);
        row.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
    }

    /** Fila KPI con valor Long (numérico). */
    private void writeKpiRow(Sheet sheet, int ri,
                             String label, Long value, Styles s) {
        Row row = sheet.createRow(ri);
        CellStyle cs = rowStyle(ri, s);
        applyStr(row.createCell(0), label, cs);
        applyNum(row.createCell(1), value, cs);
    }

    /** Fila KPI con valor String (p.ej. porcentajes). */
    private void writeKpiRowStr(Sheet sheet, int ri,
                                String label, String value, Styles s) {
        Row row = sheet.createRow(ri);
        CellStyle cs = rowStyle(ri, s);
        applyStr(row.createCell(0), label, cs);
        applyStr(row.createCell(1), value, cs);
    }

    private CellStyle rowStyle(int ri, Styles s) {
        return (ri % 2 == 0) ? s.rowAlt : s.rowPlain;
    }

    private void applyStr(Cell c, String value, CellStyle cs) {
        c.setCellValue(value);
        c.setCellStyle(cs);
    }

    private void applyNum(Cell c, Long value, CellStyle cs) {
        c.setCellValue(value != null ? value : 0L);
        c.setCellStyle(cs);
    }

    private void applyNum(Cell c, Integer value, CellStyle cs) {
        c.setCellValue(value != null ? value : 0);
        c.setCellStyle(cs);
    }

    /** Auto-ajusta columnas y aplica un mínimo de ~14 caracteres de ancho. */
    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3500) {
                sheet.setColumnWidth(i, 3500);
            }
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String nz(String v) { return v != null ? v : ""; }

    // ──────────────────────────────────────────────────────────────────────────────
    //  ENCABEZADO INSTITUCIONAL (logo + nombre)
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Agrega un encabezado institucional con logo y nombre en la hoja de Excel.
     * Retorna la fila siguiente disponible para continuar escribiendo.
     *
     * @param wb             workbook para agregar la imagen
     * @param sheet          hoja donde se escribe el encabezado
     * @param startRow       fila inicial
     * @param lastCol        última columna para merge del nombre
     * @param logoUrl        URL del logo (desencriptada)
     * @param institutionName nombre de la institución
     * @return la fila siguiente disponible después del encabezado
     */
    private int addInstitutionHeaderToSheet(XSSFWorkbook wb, XSSFSheet sheet, int startRow, int lastCol,
                                            String logoUrl, String institutionName) {
        if ((logoUrl == null || logoUrl.isBlank()) && (institutionName == null || institutionName.isBlank())) {
            return startRow;
        }

        int ri = startRow;

        // Logo
        if (logoUrl != null && !logoUrl.isBlank()) {
            try (InputStream is = new URL(logoUrl).openStream()) {
                byte[] imageBytes = IOUtils.toByteArray(is);
                int pictureIdx = wb.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                CreationHelper helper = wb.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(ri);
                anchor.setCol2(1);
                anchor.setRow2(ri + 4);
                drawing.createPicture(anchor, pictureIdx);
            } catch (Exception e) {
                // Si falla la descarga del logo, continuar sin él
            }
        }

        // Nombre de la institución al lado del logo
        if (institutionName != null && !institutionName.isBlank()) {
            Row nameRow = sheet.createRow(ri + 1);
            nameRow.setHeightInPoints(24);
            Cell nameCell = nameRow.createCell(1);
            nameCell.setCellValue(institutionName);
            CellStyle instStyle = wb.createCellStyle();
            Font instFont = wb.createFont();
            instFont.setBold(true);
            instFont.setFontHeightInPoints((short) 14);
            instFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            instStyle.setFont(instFont);
            instStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            nameCell.setCellStyle(instStyle);
            if (lastCol > 1) {
                sheet.addMergedRegion(new CellRangeAddress(ri + 1, ri + 1, 1, lastCol));
            }
        }

        return ri + 5; // dejar espacio para el logo (4 filas) + 1 de margen
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  TABLA SIMPLE — replica la vista previa del frontend
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Genera un archivo Excel de una sola hoja con los mismos datos y columnas
     * que aparecen en la vista previa del frontend.
     */
    public byte[] generateSimpleTable(List<java.util.Map<String, Object>> rows,
                                      List<String> keys,
                                      List<String> headers,
                                      String title,
                                      String periodName,
                                      String institutionName,
                                      String institutionLogoUrl) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            SimpleStyles ss = new SimpleStyles(wb);
            XSSFSheet sheet = wb.createSheet(title.length() > 31 ? title.substring(0, 31) : title);
            int ri = 0;

            // ── Encabezado institucional ─────────────────────────────────────
            ri = addInstitutionHeaderToSheet(wb, sheet, ri, Math.max(headers.size() - 1, 1),
                    institutionLogoUrl, institutionName);

            // ── Fila de título ──────────────────────────────────────────────
            int titleRowIdx = ri;
            Row titleRow = sheet.createRow(ri++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title + " — " + periodName);
            titleCell.setCellStyle(ss.title);
            if (headers.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(titleRowIdx, titleRowIdx, 0, headers.size() - 1));
            }

            // ── Fila de encabezados ─────────────────────────────────────────
            Row headerRow = sheet.createRow(ri++);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(ss.header);
            }

            // ── Filas de datos ──────────────────────────────────────────────
            for (java.util.Map<String, Object> row : rows) {
                Row dataRow = sheet.createRow(ri);
                CellStyle cs = (ri % 2 == 0) ? ss.rowAlt : ss.rowPlain;
                for (int i = 0; i < keys.size(); i++) {
                    Object val = row.get(keys.get(i));
                    Cell c = dataRow.createCell(i);
                    if (val instanceof Number) {
                        c.setCellValue(((Number) val).doubleValue());
                    } else {
                        c.setCellValue(val != null ? val.toString() : "");
                    }
                    c.setCellStyle(cs);
                }
                ri++;
            }

            autoSize(sheet, headers.size());
            return toBytes(wb);
        }
    }

    /** Estilos en verde (#1B7505) para coincidir con el PDF del frontend. */
    private static final class SimpleStyles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle rowPlain;
        final CellStyle rowAlt;

        SimpleStyles(XSSFWorkbook wb) {
            title    = buildTitle(wb);
            header   = buildHeader(wb);
            rowPlain = buildRow(wb, false);
            rowAlt   = buildRow(wb, true);
        }

        private static CellStyle buildTitle(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 14);
            f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
            s.setFont(f);
            s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)27,(byte)117,(byte)5}, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        private static CellStyle buildHeader(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
            s.setFont(f);
            s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)27,(byte)117,(byte)5}, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            addBordersXssf(s);
            return s;
        }

        private static CellStyle buildRow(XSSFWorkbook wb, boolean alt) {
            XSSFCellStyle s = wb.createCellStyle();
            if (alt) {
                s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)245,(byte)250,(byte)245}, null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            addBordersXssf(s);
            return s;
        }

        private static void addBordersXssf(XSSFCellStyle s) {
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  ESTILOS (creados una sola vez por workbook)
    // ──────────────────────────────────────────────────────────────────────────

    private static final class Styles {

        final CellStyle title;
        final CellStyle header;
        final CellStyle rowPlain;
        final CellStyle rowAlt;

        Styles(Workbook wb) {
            title    = buildTitle(wb);
            header   = buildHeader(wb);
            rowPlain = buildRow(wb, false);
            rowAlt   = buildRow(wb, true);
        }

        /** Título: azul marino, letra blanca, negrita 14 pt, centrado. */
        private static CellStyle buildTitle(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 14);
            f.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(f);
            s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        /** Encabezado de tabla: azul marino, letra blanca, negrita, centrado, bordes. */
        private static CellStyle buildHeader(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            f.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(f);
            s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            addBorders(s);
            return s;
        }

        /** Fila de datos: blanca o azul claro para efecto cebra. */
        private static CellStyle buildRow(Workbook wb, boolean alt) {
            CellStyle s = wb.createCellStyle();
            if (alt) {
                s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            addBorders(s);
            return s;
        }

        private static void addBorders(CellStyle s) {
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }
}
