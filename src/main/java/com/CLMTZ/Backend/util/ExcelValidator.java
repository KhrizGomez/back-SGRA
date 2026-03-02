package com.CLMTZ.Backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import com.CLMTZ.Backend.dto.academic.SyllabiLoadDTO;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class ExcelValidator {

    /**
     * 1. Detecta "Filas Fantasma" (filas que Excel cree que existen pero están vacías)
     */
    public static boolean isRowEmpty(Row row) {
        if (row == null || row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                // Si la celda tiene un valor real de texto o número, no está vacía
                if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty()) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 2. Valida que un campo de texto no esté vacío ni sea solo espacios
     */
    public static boolean isValidText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 3. Valida Cédulas (Exige exactamente 10 dígitos numéricos)
     */
    public static boolean isValidCedula(String value) {
        if (value == null) return false;
        String cleanValue = value.trim();
        return cleanValue.matches("^\\d{10}$");
    }

    /**
     * 4. Valida Fechas en formato estricto (Ej: YYYY-MM-DD)
     */
    public static String normalizeDate(String value) {  
        if (value == null || value.trim().isEmpty()) return "";
        
        // 1. Limpiamos espacios y cambiamos barras por guiones para estandarizar a internamente
        String cleanValue = value.trim().replace("/", "-");

        // CASO 1: Ya está perfecto (AAAA-MM-DD) -> Ej: 2026-05-01
        if (cleanValue.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return cleanValue;
        }

        // CASO 2: Formato Latinoamericano completo (DD-MM-AAAA) -> Ej: 10-08-2013 o 5-8-2013
        if (cleanValue.matches("^\\d{1,2}-\\d{1,2}-\\d{4}$")) {
            try {
                // Intento 1: Día-Mes-Año (d-M-yyyy)
                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("d-M-yyyy");
                return LocalDate.parse(cleanValue, formatter1).toString();
            } catch (DateTimeParseException e1) {
                try {
                    // Intento 2: Mes-Día-Año (M-d-yyyy) - Formato EE.UU.
                    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("M-d-yyyy");
                    return LocalDate.parse(cleanValue, formatter2).toString();
                } catch (DateTimeParseException e2) {
                    return cleanValue; // Falla todo, lo devuelve tal cual
                }
            }
        }

        // CASO 3: Formato corto de Excel (DD-MM-AA) -> Ej: 10-08-13 (El que me pediste)
        if (cleanValue.matches("^\\d{1,2}-\\d{1,2}-\\d{2}$")) {
            try {
                // Intento 1: Día-Mes-Año (d-M-yy)
                DateTimeFormatter formatter1 = new DateTimeFormatterBuilder()
                        .appendPattern("d-M-")
                        .appendValueReduced(ChronoField.YEAR, 2, 2, 2000) 
                        .toFormatter();
                return LocalDate.parse(cleanValue, formatter1).toString(); 
            } catch (Exception e1) {
                try {
                    // Intento 2: Mes-Día-Año (M-d-yy) - Formato EE.UU.
                    DateTimeFormatter formatter2 = new DateTimeFormatterBuilder()
                            .appendPattern("M-d-")
                            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000) 
                            .toFormatter();
                    return LocalDate.parse(cleanValue, formatter2).toString();
                } catch (Exception e2) {
                    return cleanValue; // Falla todo
                }
            }
        }

        // Si no coincide con ninguno, lo devolvemos tal cual para que el validador estricto lo rechace luego
        return cleanValue;
    }

    /**
     * 5. Valida Números Enteros (Ideal para Semestres, Unidades, Días de la semana)
     */
    public static boolean isValidInteger(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            // Si viene con decimales de Excel (ej: "3.0"), lo cortamos
            String cleanValue = value.contains(".") ? value.substring(0, value.indexOf('.')) : value;
            Integer.parseInt(cleanValue.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 6. Valida Formato de Hora (HH:mm) para los Horarios de Clase
     */
    public static boolean isValidTime(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            String timeStr = value.trim();
            // Limpieza básica (si mandan 8:00, lo pasamos a 08:00)
            if (timeStr.matches("^\\d:\\d{2}.*")) timeStr = "0" + timeStr;
            if (timeStr.length() > 5) timeStr = timeStr.substring(0, 5);
            
            LocalTime.parse(timeStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static void validarYCorregir(SyllabiLoadDTO dto) {
        if (dto.getCarreraTexto() != null) dto.setCarreraTexto(dto.getCarreraTexto().trim());
        if (dto.getAsignaturaTexto() != null) dto.setAsignaturaTexto(dto.getAsignaturaTexto().trim());
        if (dto.getNombreTema() != null) dto.setNombreTema(dto.getNombreTema().trim());

        // 2. Validar que no falten datos cruciales
        if (dto.getCarreraTexto() == null || dto.getCarreraTexto().isEmpty()) {
            throw new RuntimeException("El nombre de la carrera no puede estar vacío.");
        }
        if (dto.getAsignaturaTexto() == null || dto.getAsignaturaTexto().isEmpty()) {
            throw new RuntimeException("El nombre de la asignatura no puede estar vacío.");
        }
        if (dto.getNombreTema() == null || dto.getNombreTema().isEmpty()) {
            throw new RuntimeException("El nombre del tema no puede estar vacío.");
        }
        if (dto.getUnidad() == null || dto.getUnidad() <= 0) {
            throw new RuntimeException("La unidad del temario debe ser un número válido mayor a 0.");
        }
    }
}