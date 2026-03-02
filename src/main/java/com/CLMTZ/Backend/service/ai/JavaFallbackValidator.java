package com.CLMTZ.Backend.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.ai.AIValidationIssue;
import com.CLMTZ.Backend.dto.ai.AIValidationResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Validador de respaldo basado en reglas Java puras.
 * Se usa cuando la API de Groq no esta disponible, falla, o excede el timeout.
 *
 * Implementa validaciones:
 * - Campos obligatorios vacios/nulos
 * - Formatos de cedula ecuatoriana (10 digitos)
 * - Formatos de correo electronico (regex)
 * - Duplicados exactos dentro del mismo Excel
 * - Rangos de valores (dia de semana 1-7, horas validas, etc.)
 * - Validación de idioma y caracteres extraños
 */
@Slf4j
@Service
public class JavaFallbackValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    private static final Pattern CEDULA_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern ONLY_LETTERS_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$");
    private static final Pattern STRANGE_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ@.\\s_/-]");

    /**
     * Ejecuta todas las validaciones de fallback sobre las filas del Excel.
     *
     * @param rows           filas del Excel como mapas clave-valor
     * @param loadType       tipo de carga ("students", "teachers", "class_schedules", etc.)
     * @param requiredFields campos obligatorios para este tipo de carga
     * @return resultado de validacion con los issues encontrados
     */
    public AIValidationResult validate(List<Map<String, Object>> rows, String loadType, List<String> requiredFields) {
        log.info("[JavaFallbackValidator] Ejecutando validacion fallback para tipo: {} con {} filas", loadType, rows.size());
        long startTime = System.currentTimeMillis();

        List<AIValidationIssue> issues = new ArrayList<>();

        // Ejecutar todas las validaciones
        validateRequiredFields(rows, requiredFields, issues);
        validateFormats(rows, loadType, issues);
        validateDuplicates(rows, loadType, issues);
        validateRanges(rows, loadType, issues);
        validateStrangeContent(rows, issues);

        long elapsed = System.currentTimeMillis() - startTime;

        // Determinar acción recomendada
        boolean hasErrors = issues.stream().anyMatch(i -> i.getSeverity() == AIValidationIssue.Severity.ERROR);
        boolean hasWarnings = issues.stream().anyMatch(i -> i.getSeverity() == AIValidationIssue.Severity.WARNING);

        String action;
        if (hasErrors) {
            action = "REJECT";
        } else if (hasWarnings) {
            action = "REVIEW";
        } else {
            action = "PROCEED";
        }

        // Construir resumen
        long errorCount = issues.stream().filter(i -> i.getSeverity() == AIValidationIssue.Severity.ERROR).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == AIValidationIssue.Severity.WARNING).count();
        long infoCount = issues.stream().filter(i -> i.getSeverity() == AIValidationIssue.Severity.INFO).count();

        String summary;
        if (issues.isEmpty()) {
            summary = "✅ Validación completada exitosamente. No se encontraron problemas en las " + rows.size() + " filas analizadas.";
        } else {
            summary = String.format("Validación completada: %d errores, %d advertencias, %d sugerencias en %d filas.",
                    errorCount, warningCount, infoCount, rows.size());
        }

        return AIValidationResult.builder()
                .issues(issues)
                .aiValidated(false)
                .recommendedAction(action)
                .summary(summary)
                .validationTimeMs(elapsed)
                .build();
    }

    /**
     * Valida que los campos obligatorios no esten vacios o nulos.
     */
    protected void validateRequiredFields(List<Map<String, Object>> rows, List<String> requiredFields,
                                          List<AIValidationIssue> issues) {
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNum = i + 1;

            for (String field : requiredFields) {
                Object value = row.get(field);
                if (value == null || value.toString().trim().isEmpty()) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field(field)
                            .severity(AIValidationIssue.Severity.ERROR)
                            .message("El campo '" + field + "' está vacío o no existe en la fila " + rowNum + ". Este campo es obligatorio.")
                            .suggestion("Revise el archivo Excel y asegúrese de que la columna '" + field + "' tenga un valor válido.")
                            .source("FALLBACK")
                            .build());
                }
            }
        }
    }

    /**
     * Valida formatos especificos segun el tipo de carga (cedula, correo, etc.).
     */
    protected void validateFormats(List<Map<String, Object>> rows, String loadType,
                                   List<AIValidationIssue> issues) {
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNum = i + 1;

            // Validar cédula/identificación
            String cedulaField = getCedulaFieldForType(loadType);
            if (cedulaField != null && row.containsKey(cedulaField)) {
                String cedula = String.valueOf(row.get(cedulaField)).trim();
                if (!cedula.isEmpty() && !CEDULA_PATTERN.matcher(cedula).matches()) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field(cedulaField)
                            .severity(AIValidationIssue.Severity.ERROR)
                            .message("La identificación '" + cedula + "' en la fila " + rowNum + " no tiene el formato correcto. Debe tener exactamente 10 dígitos numéricos.")
                            .suggestion("Verifique que la cédula tenga 10 dígitos sin espacios ni caracteres especiales. Valor actual: " + cedula)
                            .source("FALLBACK")
                            .build());
                }
            }

            // Validar correo electrónico
            if (row.containsKey("correo")) {
                String correo = String.valueOf(row.get("correo")).trim();
                if (!correo.isEmpty() && !EMAIL_PATTERN.matcher(correo).matches()) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field("correo")
                            .severity(AIValidationIssue.Severity.ERROR)
                            .message("El correo '" + correo + "' en la fila " + rowNum + " no tiene un formato válido.")
                            .suggestion("El correo debe tener formato: usuario@dominio.ext. Ejemplo: nombre@uteq.edu.ec")
                            .source("FALLBACK")
                            .build());
                }
            }

            // Validar nombres y apellidos (solo letras y espacios)
            validateNameField(row, "nombres", rowNum, issues);
            validateNameField(row, "apellidos", rowNum, issues);
        }
    }

    private void validateNameField(Map<String, Object> row, String fieldName, int rowNum, List<AIValidationIssue> issues) {
        if (row.containsKey(fieldName)) {
            String value = String.valueOf(row.get(fieldName)).trim();
            if (!value.isEmpty()) {
                // Verificar caracteres extraños
                if (!ONLY_LETTERS_PATTERN.matcher(value).matches()) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field(fieldName)
                            .severity(AIValidationIssue.Severity.WARNING)
                            .message("El campo '" + fieldName + "' en la fila " + rowNum + " contiene caracteres no alfabéticos: '" + value + "'")
                            .suggestion("Este campo debería contener solo letras y espacios. Verifique si hay números o símbolos incorrectos.")
                            .source("FALLBACK")
                            .build());
                }
                // Verificar si es muy corto
                if (value.length() < 2) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field(fieldName)
                            .severity(AIValidationIssue.Severity.WARNING)
                            .message("El campo '" + fieldName + "' en la fila " + rowNum + " parece muy corto: '" + value + "'")
                            .suggestion("Verifique que el nombre o apellido esté completo.")
                            .source("FALLBACK")
                            .build());
                }
            }
        }
    }

    /**
     * Detecta filas duplicadas exactas dentro del mismo Excel.
     */
    protected void validateDuplicates(List<Map<String, Object>> rows, String loadType,
                                      List<AIValidationIssue> issues) {
        String keyField = getKeyFieldForType(loadType);
        if (keyField == null) return;

        Map<String, List<Integer>> valueToRows = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Object keyValue = row.get(keyField);
            if (keyValue != null && !keyValue.toString().trim().isEmpty()) {
                String key = keyValue.toString().trim().toLowerCase();
                valueToRows.computeIfAbsent(key, k -> new ArrayList<>()).add(i + 1);
            }
        }

        // Reportar duplicados
        for (Map.Entry<String, List<Integer>> entry : valueToRows.entrySet()) {
            List<Integer> rowNumbers = entry.getValue();
            if (rowNumbers.size() > 1) {
                String duplicateValue = entry.getKey();
                issues.add(AIValidationIssue.builder()
                        .row(rowNumbers.get(0))
                        .field(keyField)
                        .severity(AIValidationIssue.Severity.WARNING)
                        .message("Se encontró un valor duplicado en el campo '" + keyField + "': '" + duplicateValue + "'. Aparece en las filas: " + rowNumbers)
                        .suggestion("Verifique si estos registros son duplicados accidentales o si cada uno representa una entidad diferente.")
                        .source("FALLBACK")
                        .build());
            }
        }
    }

    /**
     * Valida que los valores numericos esten en rangos validos.
     */
    protected void validateRanges(List<Map<String, Object>> rows, String loadType,
                                  List<AIValidationIssue> issues) {
        if (!"class_schedules".equals(loadType)) return;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNum = i + 1;

            // Validar día de la semana (1-7)
            if (row.containsKey("diaSemana")) {
                try {
                    int dia = Integer.parseInt(String.valueOf(row.get("diaSemana")).trim());
                    if (dia < 1 || dia > 7) {
                        issues.add(AIValidationIssue.builder()
                                .row(rowNum)
                                .field("diaSemana")
                                .severity(AIValidationIssue.Severity.ERROR)
                                .message("El día de la semana en la fila " + rowNum + " es inválido: " + dia + ". Debe estar entre 1 (Lunes) y 7 (Domingo).")
                                .suggestion("Use valores del 1 al 7: 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes, 6=Sábado, 7=Domingo")
                                .source("FALLBACK")
                                .build());
                    }
                } catch (NumberFormatException e) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field("diaSemana")
                            .severity(AIValidationIssue.Severity.ERROR)
                            .message("El día de la semana en la fila " + rowNum + " no es un número válido: " + row.get("diaSemana"))
                            .suggestion("Ingrese un número del 1 al 7 para representar el día de la semana.")
                            .source("FALLBACK")
                            .build());
                }
            }
        }
    }

    /**
     * Detecta contenido extraño o en idioma incorrecto.
     */
    protected void validateStrangeContent(List<Map<String, Object>> rows, List<AIValidationIssue> issues) {
        Set<String> reportedFields = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNum = i + 1;

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();

                if (value == null) continue;
                String strValue = value.toString().trim();
                if (strValue.isEmpty()) continue;

                // Detectar caracteres extraños (símbolos no comunes)
                if (STRANGE_CHARS_PATTERN.matcher(strValue).find() && !reportedFields.contains(field + "_" + rowNum)) {
                    String strangeChars = strValue.replaceAll("[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ@.\\s_/-]", "");
                    if (!strangeChars.isEmpty()) {
                        issues.add(AIValidationIssue.builder()
                                .row(rowNum)
                                .field(field)
                                .severity(AIValidationIssue.Severity.INFO)
                                .message("El campo '" + field + "' en la fila " + rowNum + " contiene caracteres especiales: '" + strangeChars + "' en valor: '" + strValue + "'")
                                .suggestion("Verifique si estos caracteres son correctos o si son errores de codificación.")
                                .source("FALLBACK")
                                .build());
                        reportedFields.add(field + "_" + rowNum);
                    }
                }

                // Detectar espacios extras al inicio o final
                String originalValue = value.toString();
                if (!originalValue.equals(strValue) && !reportedFields.contains(field + "_spaces_" + rowNum)) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field(field)
                            .severity(AIValidationIssue.Severity.INFO)
                            .message("El campo '" + field + "' en la fila " + rowNum + " tiene espacios en blanco adicionales al inicio o al final.")
                            .suggestion("Elimine los espacios extra del valor: '" + originalValue + "'")
                            .source("FALLBACK")
                            .build());
                    reportedFields.add(field + "_spaces_" + rowNum);
                }
            }
        }
    }

    private String getCedulaFieldForType(String loadType) {
        return switch (loadType) {
            case "students" -> "identificacion";
            case "teachers" -> "cedula";
            case "class_schedules" -> "cedulaDocente";
            case "registrations" -> "cedulaEstudiante";
            default -> null;
        };
    }

    private String getKeyFieldForType(String loadType) {
        return switch (loadType) {
            case "students" -> "identificacion";
            case "teachers" -> "nombreCompleto";
            case "registrations" -> "cedulaEstudiante";
            default -> null;
        };
    }
}
