package com.CLMTZ.Backend.util;

import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelHelperStudentsTest {

    @Test
    void excelToStudentsBatch_incluyeLaUltimaFilaDeDatos() throws IOException {
        try (InputStream inputStream = buildStudentsWorkbook()) {
            int totalRows = ExcelHelper.countStudentRows(inputStream, "Software", "Presencial");
            assertEquals(3, totalRows);
        }

        try (InputStream inputStream = buildStudentsWorkbook()) {
            List<StudentLoadDTO> students = ExcelHelper.excelToStudentsBatch(
                    inputStream,
                    0,
                    10,
                    "Software",
                    "Presencial"
            );

            assertEquals(3, students.size());
            assertEquals("BELINDA BETZABETH TOAQUIZA ZAMBRANO", students.get(1).getNombres() + " " + students.get(1).getApellidos());
            assertEquals("LUIS MARIO", students.get(2).getNombres());
            assertEquals("ZAMBRANO PARRAGA", students.get(2).getApellidos());
            assertEquals("0651080749", students.get(2).getIdentificacion());
        }
    }

    private InputStream buildStudentsWorkbook() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        sheet.createRow(0).createCell(0).setCellValue("UNIVERSIDAD TECNICA ESTATAL DE QUEVEDO");
        sheet.createRow(2).createCell(0).setCellValue("ESTUDIANTE");
        sheet.getRow(2).createCell(1).setCellValue("IDENTIFICACION");
        sheet.getRow(2).createCell(2).setCellValue("EMAIL INSTITUCIONAL");
        sheet.getRow(2).createCell(3).setCellValue("TELEFONO");

        sheet.createRow(3).createCell(0).setCellValue("NIURCA SCARLETH BONE ARROYO");
        sheet.getRow(3).createCell(1).setCellValue("0605414374");
        sheet.getRow(3).createCell(2).setCellValue("nbonea@uteq.edu.ec");
        sheet.getRow(3).createCell(3).setCellValue("0998975399");

        sheet.createRow(12).createCell(0).setCellValue("BELINDA BETZABETH TOAQUIZA ZAMBRANO");
        sheet.getRow(12).createCell(1).setCellValue("1207120328");
        sheet.getRow(12).createCell(2).setCellValue("btoaquizza@uteq.edu.ec");
        sheet.getRow(12).createCell(3).setCellValue("0998975399");

        sheet.createRow(13).createCell(0).setCellValue("LUIS MARIO ZAMBRANO PARRAGA");
        sheet.getRow(13).createCell(1).setCellValue("0651080749");
        sheet.getRow(13).createCell(2).setCellValue("lzambranop6@uteq.edu.ec");
        sheet.getRow(13).createCell(3).setCellValue("0998975399");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}