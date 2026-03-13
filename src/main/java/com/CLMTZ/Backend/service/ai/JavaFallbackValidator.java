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
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
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
            int rowNum = getRowNumber(row, i);

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
            int rowNum = getRowNumber(row, i);

            // Validar identificacion (cedula ecuatoriana o pasaporte)
            String idField = getIdFieldForType(loadType);
            if (idField != null && row.containsKey(idField)) {
                String id = getString(row, idField);
                if (!id.isEmpty()) {
                    validateIdentificacion(id, rowNum, idField, issues);
                }
            }

            if ("students".equals(loadType)) {
                validateNameLikeField(row, "nombre_completo", 2, rowNum, issues);
                validateEmailField(row, "correo", rowNum, issues);
                validatePhoneField(row, "telefono1", true, rowNum, issues);
                validatePhoneField(row, "telefono2", false, rowNum, issues);

                String nombre = getString(row, "nombre_completo");
                String identificacion = getString(row, "identificacion");
                if (!nombre.isEmpty() && !identificacion.isEmpty() && nombre.equalsIgnoreCase(identificacion)) {
                    issues.add(AIValidationIssue.builder()
                            .row(rowNum)
                            .field("nombre_completo")
                            .severity(AIValidationIssue.Severity.WARNING)
                            .message("El nombre completo es igual a la identificacion en la fila " + rowNum + ".")
                            .suggestion("Verifique si el campo nombre_completo fue llenado correctamente.")
                            .source("FALLBACK")
                            .build());
                }
            } else if ("teachers".equals(loadType)) {
                validateNameLikeField(row, "profesor", 2, rowNum, issues);
                validateEmailField(row, "correo", rowNum, issues);
            } else if ("registrations".equals(loadType)) {
                validateNameLikeField(row, "nombres", 1, rowNum, issues);
                validateNameLikeField(row, "apellidos", 1, rowNum, issues);
                validateEmailField(row, "email", rowNum, issues);
                validateSexoField(row, "sexo", rowNum, issues);
            } else {
                validateEmailField(row, "correo", rowNum, issues);
                validateEmailField(row, "email", rowNum, issues);
                validateNameLikeField(row, "nombres", 1, rowNum, issues);
                validateNameLikeField(row, "apellidos", 1, rowNum, issues);
            }
        }
    }

    private void validateNameLikeField(Map<String, Object> row, String fieldName, int minWords,
                                       int rowNum, List<AIValidationIssue> issues) {
        if (!row.containsKey(fieldName)) return;
        String value = getString(row, fieldName);
        if (value.isEmpty()) return;

        if (!ONLY_LETTERS_PATTERN.matcher(value).matches()) {
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(fieldName)
                    .severity(AIValidationIssue.Severity.WARNING)
                    .message("El campo '" + fieldName + "' en la fila " + rowNum + " contiene caracteres no alfabeticos: '" + value + "'")
                    .suggestion("Este campo deberia contener solo letras y espacios.")
                    .source("FALLBACK")
                    .build());
        }

        int wordCount = value.trim().split("\\s+").length;
        if (wordCount < minWords) {
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(fieldName)
                    .severity(AIValidationIssue.Severity.WARNING)
                    .message("El campo '" + fieldName + "' en la fila " + rowNum + " parece incompleto: '" + value + "'")
                    .suggestion("Verifique que el nombre o apellido este completo.")
                    .source("FALLBACK")
                    .build());
        }
    }

    private void validateEmailField(Map<String, Object> row, String fieldName, int rowNum,
                                    List<AIValidationIssue> issues) {
        if (!row.containsKey(fieldName)) return;
        String correo = getString(row, fieldName);
        if (!correo.isEmpty() && !EMAIL_PATTERN.matcher(correo).matches()) {
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(fieldName)
                    .severity(AIValidationIssue.Severity.ERROR)
                    .message("El correo '" + correo + "' en la fila " + rowNum + " no tiene un formato valido.")
                    .suggestion("El correo debe tener formato: usuario@dominio.ext. Ejemplo: nombre@uteq.edu.ec")
                    .source("FALLBACK")
                    .build());
        }
    }

    private void validatePhoneField(Map<String, Object> row, String fieldName, boolean required,
                                    int rowNum, List<AIValidationIssue> issues) {
        if (!row.containsKey(fieldName)) return;
        String raw = getString(row, fieldName);
        if (raw.isEmpty()) return;

        String digits = raw.replaceAll("\\D", "");
        boolean isValid = digits.length() == 10 && digits.startsWith("0");
        if (!isValid) {
            AIValidationIssue.Severity severity = required
                    ? AIValidationIssue.Severity.ERROR
                    : AIValidationIssue.Severity.WARNING;
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(fieldName)
                    .severity(severity)
                    .message("El telefono '" + raw + "' en la fila " + rowNum + " no tiene 10 digitos validos.")
                    .suggestion("Use un telefono de 10 digitos, por ejemplo 0999999999.")
                    .source("FALLBACK")
                    .build());
        }
    }

    private void validateSexoField(Map<String, Object> row, String fieldName, int rowNum,
                                   List<AIValidationIssue> issues) {
        if (!row.containsKey(fieldName)) return;
        String sexo = getString(row, fieldName).toUpperCase();
        if (sexo.isEmpty()) return;
        String first = sexo.substring(0, 1);
        if (!first.equals("M") && !first.equals("F")) {
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(fieldName)
                    .severity(AIValidationIssue.Severity.ERROR)
                    .message("El sexo en la fila " + rowNum + " es invalido: '" + sexo + "'.")
                    .suggestion("Use valores que inicien con M o F (ej. M, F, MASCULINO, FEMENINO).")
                    .source("FALLBACK")
                    .build());
        }
    }
    /**
     * Detecta filas duplicadas exactas dentro del mismo Excel.
     */
    protected void validateDuplicates(List<Map<String, Object>> rows, String loadType,
                                      List<AIValidationIssue> issues) {
        Map<String, List<Integer>> valueToRows = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String key = buildDuplicateKey(loadType, row);
            if (key == null || key.isEmpty()) continue;

            int rowNum = getRowNumber(row, i);
            valueToRows.computeIfAbsent(key, k -> new ArrayList<>()).add(rowNum);
        }

        String fieldLabel = getDuplicateFieldLabel(loadType);
        for (Map.Entry<String, List<Integer>> entry : valueToRows.entrySet()) {
            List<Integer> rowNumbers = entry.getValue();
            if (rowNumbers.size() > 1) {
                String duplicateValue = entry.getKey();
                issues.add(AIValidationIssue.builder()
                        .row(rowNumbers.get(0))
                        .field(fieldLabel)
                        .severity(AIValidationIssue.Severity.WARNING)
                        .message("Se encontro un valor duplicado en el campo '" + fieldLabel + "': '" + duplicateValue + "'. Aparece en las filas: " + rowNumbers)
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
            int rowNum = getRowNumber(row, i);

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
            int rowNum = getRowNumber(row, i);

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

    private String getIdFieldForType(String loadType) {
        return switch (loadType) {
            case "students" -> "identificacion";
            case "registrations" -> "identificacion";
            case "class_schedules" -> "cedulaDocente";
            default -> null;
        };
    }

    private String buildDuplicateKey(String loadType, Map<String, Object> row) {
        return switch (loadType) {
            case "students" -> normalizeKey(getString(row, "identificacion"));
            case "teachers" -> {
                String profesor = getString(row, "profesor");
                String materia = getString(row, "materia");
                String paralelo = getString(row, "paralelo");
                String carrera = getString(row, "carrera");
                String coordinacion = getString(row, "coordinacion");
                String nivel = getString(row, "nivel");
                if (profesor.isEmpty() || materia.isEmpty() || paralelo.isEmpty()) {
                    yield null;
                }
                String key = String.join("|", profesor, materia, paralelo, carrera, coordinacion, nivel);
                yield normalizeKey(key);
            }
            case "registrations" -> normalizeKey(getString(row, "identificacion"));
            default -> null;
        };
    }

    private String getDuplicateFieldLabel(String loadType) {
        return switch (loadType) {
            case "students" -> "identificacion";
            case "teachers" -> "profesor|materia|paralelo";
            case "registrations" -> "identificacion";
            default -> "registro";
        };
    }

    private int getRowNumber(Map<String, Object> row, int index) {
        Object fila = row.get("fila");
        if (fila != null) {
            try {
                return Integer.parseInt(fila.toString().trim());
            } catch (NumberFormatException ignored) {
                // fallback abajo
            }
        }
        return index + 1;
    }

    private String getString(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? "" : value.toString().trim();
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void validateIdentificacion(String identificacion, int rowNum, String field,
                                        List<AIValidationIssue> issues) {
        String value = identificacion.trim();

        if (CEDULA_PATTERN.matcher(value).matches()) {
            if (!isValidEcuadorianCedula(value)) {
                issues.add(AIValidationIssue.builder()
                        .row(rowNum)
                        .field(field)
                        .severity(AIValidationIssue.Severity.ERROR)
                        .message("La cedula en la fila " + rowNum + " no es valida: '" + value + "'.")
                        .suggestion("Verifique provincia, tercer digito y digito verificador.")
                        .source("FALLBACK")
                        .build());
            }
            return;
        }

        if (!PASSPORT_PATTERN.matcher(value).matches()) {
            issues.add(AIValidationIssue.builder()
                    .row(rowNum)
                    .field(field)
                    .severity(AIValidationIssue.Severity.ERROR)
                    .message("La identificacion en la fila " + rowNum + " no cumple formato de cedula ni pasaporte: '" + value + "'.")
                    .suggestion("Use cedula ecuatoriana de 10 digitos o pasaporte alfanumerico (5-20) sin espacios.")
                    .source("FALLBACK")
                    .build());
        }
    }

    private boolean isValidEcuadorianCedula(String cedula) {
        if (cedula == null || !CEDULA_PATTERN.matcher(cedula).matches()) return false;

        int province = Integer.parseInt(cedula.substring(0, 2));
        if (province < 1 || province > 24) return false;

        int third = Character.getNumericValue(cedula.charAt(2));
        if (third < 0 || third > 5) return false;

        int[] coef = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(cedula.charAt(i));
            int prod = digit * coef[i];
            sum += (prod >= 10) ? (prod - 9) : prod;
        }
        int check = (10 - (sum % 10)) % 10;
        int verifier = Character.getNumericValue(cedula.charAt(9));
        return check == verifier;
    }
}
