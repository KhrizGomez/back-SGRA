package com.CLMTZ.Backend.util;

import com.CLMTZ.Backend.dto.academic.CareerLoadDTO;
import com.CLMTZ.Backend.dto.academic.ClassScheduleLoadDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.SubjectLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelHelper {

    private static final DataFormatter formatter = new DataFormatter();
    public static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String TYPE_XLS = "application/vnd.ms-excel";

    // Columnas de resumen en Matricula.xlsx (no son asignaturas)
    private static final java.util.Set<String> COLUMNAS_RESUMEN = java.util.Set.of(
            "APROBADAS", "REPROBADAS", "MATRICULADAS", "PENDIENTES", "TOTALES");

    public static boolean hasExcelFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;

        String filename = file.getOriginalFilename();
        if (filename != null && (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            return true;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            return TYPE.equals(contentType) || TYPE_XLS.equals(contentType)
                    || contentType.contains("spreadsheet") || contentType.contains("excel");
        }
        return false;
    }

    // =====================================================================
    // ESTUDIANTES.xls
    // Formato:
    //   Fila 0: Encabezado institución ("UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO")
    //   Fila 1: Vacía
    //   Fila 2: Cabeceras (ESTUDIANTE | IDENTIFICACIÓN | EMAIL INSTITUCIONAL | TELÉFONO | TELÉFONO 2)
    //   Fila 3+: Datos
    //     Col 0: Nombre completo (NOMBRES APELLIDOS) ej. "DAMARYS RAELITH AGUILAR LECHON"
    //     Col 1: Identificación (cédula, pasaporte o código postal)
    //     Col 2: Email institucional ej. daguilarl3@uteq.edu.ec
    //     Col 3: Teléfono principal
    //     Col 4: Teléfono secundario (opcional)
    // Carrera y modalidad se reciben como parámetros del endpoint.
    // =====================================================================
    public static List<StudentLoadDTO> excelToStudents(InputStream is, String carreraTexto, String modalidadTexto) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<StudentLoadDTO> estudiantes = new ArrayList<>();

            // Datos empiezan en fila 3 (índice 3), filas 0-2 son encabezados
                for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String nombreCompleto = getCellValue(row, 0).trim();
                String identificacion = getCellValue(row, 1).trim();
                String correo = getCellValue(row, 2).trim();
                String telefono1 = getCellValue(row, 3).trim();
                String telefono2 = getCellValue(row, 4).trim();

                if (identificacion.isEmpty()) continue;

                // Combinar teléfonos si hay dos
                String telefono = (!telefono2.isEmpty()) ? telefono1 + " / " + telefono2 : telefono1;

                // El nombre viene en formato NOMBRES APELLIDOS: ej. "DAMARYS RAELITH AGUILAR LECHON"
                String[] partes = splitNombreApellido(nombreCompleto, false);

                StudentLoadDTO estudiante = new StudentLoadDTO();
                estudiante.setIdentificacion(identificacion);
                estudiante.setNombres(partes[0]);
                estudiante.setApellidos(partes[1]);
                estudiante.setCorreo(correo);
                estudiante.setTelefono(telefono);
                // Carrera y modalidad vienen del endpoint como parámetros
                estudiante.setCarreraTexto(carreraTexto);
                estudiante.setModalidadTexto(modalidadTexto);

                estudiantes.add(estudiante);
            }
            return estudiantes;
        } catch (IOException e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Estudiantes: " + e.getMessage());
        }
    }

    // =====================================================================
    // DOCENTE.xls
    // Formato:
    //   Fila 0: Vacía
    //   Fila 1: Cabeceras (COORDINACIÓN | CARRERA | NIVEL | MATERIA | PARALELO | PROFESOR | CORREO)
    //   Fila 2+: Datos
    //     Col 0: COORDINACIÓN (facultad/área)
    //     Col 1: CARRERA
    //     Col 2: NIVEL (ej. "1ER NIVEL")
    //     Col 3: MATERIA (asignatura)
    //     Col 4: PARALELO (ej. "B")
    //     Col 5: PROFESOR (APELLIDOS NOMBRES) ej. "BOSQUEZ MESTANZA ANGELITA LEONOR"
    //     Col 6: CORREO (email institucional, opcional)
    // =====================================================================
    public static List<TeachingDTO> excelToTeaching(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<TeachingDTO> docentes = new ArrayList<>();

            // Datos empiezan en fila 2 (índice 2), filas 0-1 son encabezados
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String coordinacion = getCellValue(row, 0).trim();
                String carrera = getCellValue(row, 1).trim();
                String nivel = getCellValue(row, 2).trim();
                String materia = getCellValue(row, 3).trim();
                String paralelo = getCellValue(row, 4).trim();
                String nombreProfesor = getCellValue(row, 5).trim();
                String correo = getCellValue(row, 6).trim();

                // Si no hay profesor, o dice "None", ignoramos la fila por completo
                if (nombreProfesor.isEmpty() || nombreProfesor.equalsIgnoreCase("None")) continue;
                if (nombreProfesor.isEmpty() && materia.isEmpty()) continue;

                // Nombre del profesor viene en APELLIDOS NOMBRES: ej. "BOSQUEZ MESTANZA ANGELITA LEONOR"
                String[] partes = splitNombreApellido(nombreProfesor, true);

                TeachingDTO docente = new TeachingDTO();
                docente.setCoordinacionTexto(coordinacion);
                docente.setCarreraTexto(carrera);
                docente.setNivelTexto(nivel);
                docente.setAsignaturaTexto(materia);
                docente.setParaleloTexto(paralelo);
                docente.setNombreCompleto(nombreProfesor);
                docente.setApellidos(partes[0]); // En Docente.xls: apellidos van primero
                docente.setNombres(partes[1]);   // nombres van segundo
                docente.setCorreo(correo);

                docentes.add(docente);
            }
            return docentes;
        } catch (IOException e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Docentes: " + e.getMessage());
        }
    }

    // =====================================================================
    // MATRICULA.xlsx  (formato de reporte de cumplimiento de malla UTEQ)
    //   Fila 0-5: Encabezados institucionales (institución, periodo, facultad, carrera, título)
    //   Fila 6:   Nombres de asignaturas en columnas 9+
    //   Fila 7:   Cabeceras de columnas:
    //             Col 0: NIVEL_MAT   Col 1: NIVEL_EST   Col 2: NIVEL_COM
    //             Col 3: IDENTIFICACIÓN   Col 4: APELLIDOS   Col 5: NOMBRES
    //             Col 6: SEXO   Col 7: EMAIL   Col 8: PARALELO (I8)   Col 9+: asignaturas
    //   Fila 8+:  Datos de estudiantes
    //             "M" en col 9+ indica que el estudiante está matriculado en esa asignatura
    //
    // Por cada celda "M" se genera un EnrollmentDetailLoadDTO.
    // El periodo activo lo resuelve el SP internamente (fn_sl_id_periodo_activo).
    // =====================================================================
    public static List<EnrollmentDetailLoadDTO> excelToEnrollmentDetails(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<EnrollmentDetailLoadDTO> detalles = new ArrayList<>();

            // El paralelo está en la celda I8 del Excel = fila índice 7, columna índice 8 (POI).
            // La celda puede contener solo la letra ("B") o un texto como "PARALELO: B".
            String paraleloGlobal = "A";
            Row filaParalelo = sheet.getRow(7);
            if (filaParalelo != null) {
                String rawParalelo = getCellValue(filaParalelo, 8).trim().toUpperCase();
                if (!rawParalelo.isEmpty()) {
                    // Si tiene "PARALELO" + letra, extraer solo la letra
                    String sinPrefijo = rawParalelo.replaceAll(".*PARALELO[:\\s]*", "").trim();
                    paraleloGlobal = sinPrefijo.isEmpty()
                            ? String.valueOf(rawParalelo.charAt(rawParalelo.length() - 1))
                            : String.valueOf(sinPrefijo.charAt(0));
                }
            }

            // Leer nombres de asignaturas de fila 6 (índice 6), columnas 9 en adelante
            Row filaAsignaturas = sheet.getRow(6);
            if (filaAsignaturas == null) {
                throw new RuntimeException("El archivo no tiene el formato esperado (falta fila de asignaturas en fila 7).");
            }
            Map<Integer, String> asignaturasPorColumna = new LinkedHashMap<>();
            for (int col = 9; col < filaAsignaturas.getLastCellNum(); col++) { // getLastCellNum() es exclusivo
                String nombreAsig = getCellValue(filaAsignaturas, col).trim();
                if (nombreAsig.isEmpty() || COLUMNAS_RESUMEN.contains(nombreAsig.toUpperCase())) continue;
                asignaturasPorColumna.put(col, nombreAsig);
            }

            // Datos de estudiantes: fila 8 (índice 8) en adelante (fila 7 = cabeceras)
            for (int i = 8; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String identificador = getCellValue(row, 3).trim(); // Col 3: IDENTIFICACIÓN
                if (identificador.isEmpty()) continue;

                // Col 6: SEXO — normalizar a primera letra para evitar errores de longitud en BD
                // ("MASCULINO"→"M", "FEMENINO"→"F", "H"→"H", etc.)
                String sexoRaw  = getCellValue(row, 6).trim().toUpperCase();
                String sexo     = sexoRaw.isEmpty() ? "" : sexoRaw.substring(0, 1);
                String nivelEst = getCellValue(row, 1).trim(); // Col 1: NIVEL_EST (ej. "1ER NIVEL")
                // El paralelo se lee de los encabezados del archivo (paraleloGlobal).
                // Col 7 es EMAIL, no paralelo.
                String paralelo = paraleloGlobal;

                // Extraer el número del nivel (ej. "1ER NIVEL" → 1, "2DO NIVEL" → 2)
                Integer semestre = 1;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(nivelEst);
                if (m.find()) {
                    try { semestre = Integer.parseInt(m.group()); } catch (NumberFormatException ignored) {}
                }

                // Por cada asignatura marcada con "M", crear un detalle
                for (Map.Entry<Integer, String> entry : asignaturasPorColumna.entrySet()) {
                    String estado = getCellValue(row, entry.getKey()).trim().toUpperCase();
                    if (!"M".equals(estado)) continue;

                    EnrollmentDetailLoadDTO detalle = new EnrollmentDetailLoadDTO();
                    detalle.setIdentificador(identificador);
                    detalle.setSexo(sexo);
                    detalle.setAsignatura(entry.getValue());
                    detalle.setSemestre(semestre);
                    detalle.setParalelo(paralelo); 
                    detalles.add(detalle);
                }
            }
            return detalles;
        } catch (IOException e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Matrícula: " + e.getMessage());
        }
    }

    // =====================================================================
    // CARRERAS (formato propio del sistema, sin cambios en estructura)
    // =====================================================================
    public static List<CareerLoadDTO> excelToCareers(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<CareerLoadDTO> careerList = new ArrayList<>();

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                CareerLoadDTO career = new CareerLoadDTO();
                career.setNombreArea(getCellValue(row, 0));
                career.setAbrevArea(getCellValue(row, 1));
                career.setNombreModalidad(getCellValue(row, 2));
                career.setNombreCarrera(getCellValue(row, 3));
                String semestresStr = getCellValue(row, 4).replace(".0", "");
                career.setSemestres(semestresStr.isEmpty() ? 0 : Short.parseShort(semestresStr));
                careerList.add(career);
            }
            return careerList;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Carreras: " + e.getMessage());
        }
    }

    // =====================================================================
    // ASIGNATURAS (formato propio del sistema, sin cambios en estructura)
    // =====================================================================
    public static List<SubjectLoadDTO> excelToSubjects(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<SubjectLoadDTO> subjectList = new ArrayList<>();

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                SubjectLoadDTO subject = new SubjectLoadDTO();
                subject.setNombreCarrera(getCellValue(row, 0));
                subject.setNombreAsignatura(getCellValue(row, 1));
                String semestreStr = getCellValue(row, 2).replace(".0", "");
                subject.setSemestre(semestreStr.isEmpty() ? 0 : Short.parseShort(semestreStr));
                subjectList.add(subject);
            }
            return subjectList;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Asignaturas: " + e.getMessage());
        }
    }

    // =====================================================================
    // HORARIOS DE CLASES (formato propio del sistema, sin cambios en estructura)
    // =====================================================================
    public static List<ClassScheduleLoadDTO> excelToClassSchedules(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ClassScheduleLoadDTO> scheduleList = new ArrayList<>();

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                ClassScheduleLoadDTO schedule = new ClassScheduleLoadDTO();
                schedule.setCedulaDocente(getCellValue(row, 0));
                schedule.setNombreAsignatura(getCellValue(row, 1));
                schedule.setNombreParalelo(getCellValue(row, 2));
                schedule.setNombrePeriodo(getCellValue(row, 3));
                String diaStr = getCellValue(row, 4).replace(".0", "");
                schedule.setDiaSemana(diaStr.isEmpty() ? 0 : Integer.parseInt(diaStr));
                schedule.setHoraInicio(parseExcelTime(getCellValue(row, 5)));
                schedule.setHoraFin(parseExcelTime(getCellValue(row, 6)));
                scheduleList.add(schedule);
            }
            return scheduleList;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear el archivo Excel de Horarios: " + e.getMessage());
        }
    }

    // =====================================================================
    // MATRICULA.xlsx
    // =====================================================================
    public static List<EnrollmentDetailLoadDTO> excelToEnrollments(InputStream is, String fileName) {
        try (Workbook workbook = fileName.endsWith(".xlsx") ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<EnrollmentDetailLoadDTO> detalles = new ArrayList<>();

            // 1. PARALELO GLOBAL (por si viene en los encabezados, celda I8)
            String paraleloGlobal = "A";
            Row filaParalelo = sheet.getRow(7);
            if (filaParalelo != null) {
                String rawParalelo = getCellValue(filaParalelo, 8).trim().toUpperCase();
                if (!rawParalelo.isEmpty()) {
                    String sinPrefijo = rawParalelo.replaceAll(".*PARALELO[:\\s]*", "").trim();
                    paraleloGlobal = sinPrefijo.isEmpty()
                            ? String.valueOf(rawParalelo.charAt(rawParalelo.length() - 1))
                            : String.valueOf(sinPrefijo.charAt(0));
                }
            }

            // 2. LEER ASIGNATURAS (Fila visual 7 → índice 6)
            Row filaAsignaturas = sheet.getRow(6);
            if (filaAsignaturas == null) {
                throw new RuntimeException("El archivo no tiene el formato esperado (falta fila de asignaturas).");
            }
            Map<Integer, String> asignaturasPorColumna = new LinkedHashMap<>();
            for (int col = 10; col < filaAsignaturas.getLastCellNum(); col++) { // getLastCellNum() es exclusivo
                String nombreAsig = getCellValue(filaAsignaturas, col).trim();
                if (nombreAsig.isEmpty() || COLUMNAS_RESUMEN.contains(nombreAsig.toUpperCase()) || nombreAsig.equals("0")) {
                    continue;
                }
                asignaturasPorColumna.put(col, nombreAsig);
            }

            // 3. LEER SEMESTRES EXACTOS POR MATERIA (Fila visual 8 → índice 7)
            Row filaNiveles = sheet.getRow(7);
            Map<Integer, Integer> semestrePorColumna = new LinkedHashMap<>();
            if (filaNiveles != null) {
                int maxCol = filaAsignaturas.getLastCellNum() - 1;
                Integer currentSemestre = 1; // carry-forward
                for (int col = 10; col <= maxCol; col++) {
                    String nivelTexto = getCellValue(filaNiveles, col).trim();
                    if (!nivelTexto.isEmpty()) {
                        java.util.regex.Matcher mNivel = java.util.regex.Pattern.compile("\\d+").matcher(nivelTexto);
                        if (mNivel.find()) {
                            try { currentSemestre = Integer.parseInt(mNivel.group()); } catch (Exception ignored) {}
                        }
                    }
                    if (asignaturasPorColumna.containsKey(col)) {
                        semestrePorColumna.put(col, currentSemestre);
                    }
                }
            }

            // 4. LEER ESTUDIANTES Y SUS MATRICULAS (Fila visual 9 → índice 8 en adelante)
            for (int i = 8; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || ExcelValidator.isRowEmpty(row)) continue;

                String identificador = getCellValue(row, 3).trim(); // Col 3: IDENTIFICACIÓN
                if (identificador.isEmpty()) continue;

                String sexoRaw  = getCellValue(row, 6).trim().toUpperCase(); // Col 6: SEXO
                String sexo     = sexoRaw.isEmpty() ? "" : sexoRaw.substring(0, 1);
                
                // LEEMOS EL PARALELO DE LA FILA BASE (Col 8)
                String paraleloBase = getCellValue(row, 8).trim().toUpperCase();
                if (paraleloBase.isEmpty()) paraleloBase = paraleloGlobal;
                if (paraleloBase.length() > 5) paraleloBase = paraleloBase.substring(0, 1);

                // RECORRER CADA MATERIA EN LA QUE PODRÍA ESTAR MATRICULADO
                for (Map.Entry<Integer, String> entry : asignaturasPorColumna.entrySet()) {
                    String estado = getCellValue(row, entry.getKey()).trim().toUpperCase();

                    // 1. FILTRO ANTI-BASURA Y ANTI-HISTORIAL
                    // Si está vacía, o tiene Aprobada (A), Reprobada (R), Pendiente (P) o un guion (-), ¡LA IGNORAMOS!
                    if (estado.isEmpty() || estado.equals("0") || estado.equals("-") ||
                        estado.equals("A") || estado.equals("R") || estado.equals("P")) {
                        continue;
                    }

                    // 2. ASIGNACIÓN DEL PARALELO REAL
                    String paraleloFinalMateria;

                    if (estado.equals("M") || estado.equals("X")) {
                        // Si pusieron la típica "M", usa el paralelo general de la columna 8
                        paraleloFinalMateria = paraleloBase;
                    } else if (estado.startsWith("M") && estado.length() == 2) {
                        // NUEVO TRUCO PARA ARRASTRES: Si ponen "MB" o "MA"
                        // Extraemos la segunda letra para saber el paralelo exacto
                        paraleloFinalMateria = estado.substring(1, 2);
                    } else if (estado.equals("B") || estado.equals("C")) {
                        // Si pusieron la letra B o C suelta, la aceptamos
                        paraleloFinalMateria = estado;
                    } else {
                        // Si tiene cualquier otra letra rara, la saltamos por seguridad
                        continue;
                    }

                    // Obtenemos el semestre EXACTO de esa materia (no el del estudiante)
                    Integer semestreExacto = semestrePorColumna.getOrDefault(entry.getKey(), 1);

                    EnrollmentDetailLoadDTO detalle = new EnrollmentDetailLoadDTO();
                    detalle.setIdentificador(identificador);
                    detalle.setSexo(sexo);
                    detalle.setAsignatura(entry.getValue());
                    detalle.setSemestre(semestreExacto);
                    detalle.setParalelo(paraleloFinalMateria);
                    detalles.add(detalle);
                }
            }
            return detalles;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear Excel de Matrículas: " + e.getMessage());
        }
    }
    
    // =====================================================================
    // UTILIDADES PRIVADAS
    // =====================================================================

    /**
     * Divide un nombre completo en [nombres, apellidos] o [apellidos, nombres].
     *
     * @param nombreCompleto el texto a dividir
     * @param apellidosPrimero true si el formato es "APELLIDOS NOMBRES" (Docente.xls),
     *                         false si es "NOMBRES APELLIDOS" (Estudiantes.xls)
     * @return arreglo de 2 elementos: [0]=primera parte, [1]=segunda parte
     */
    private static String[] splitNombreApellido(String nombreCompleto, boolean apellidosPrimero) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) {
            return new String[]{"", ""};
        }

        String[] partes = nombreCompleto.trim().split("\\s+");
        int total = partes.length;

        if (total == 1) {
            return apellidosPrimero ? new String[]{partes[0], ""} : new String[]{"", partes[0]};
        }
        if (total == 2) {
            return new String[]{partes[0], partes[1]};
        }

        // Para 3+ palabras: convención UTEQ es 2 apellidos + 2 nombres (o 2+1, 1+2, etc.)
        // Con 4 palabras: split al medio (2+2)
        // Con 3 palabras: primera parte = 2 palabras, segunda = 1
        // Con 5+: primera parte = 2 palabras, el resto va a segunda
        int splitIdx = (total >= 4) ? 2 : (total == 3 ? 2 : 1);

        StringBuilder primera = new StringBuilder();
        StringBuilder segunda = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < splitIdx) {
                if (primera.length() > 0) primera.append(" ");
                primera.append(partes[i]);
            } else {
                if (segunda.length() > 0) segunda.append(" ");
                segunda.append(partes[i]);
            }
        }

        // apellidosPrimero=true  → primera=apellidos, segunda=nombres → return [apellidos, nombres]
        // apellidosPrimero=false → primera=nombres, segunda=apellidos  → return [nombres, apellidos]
        return new String[]{primera.toString(), segunda.toString()};
    }

    private static String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (!getCellValue(row, c).isEmpty()) return false;
            }
        }
        return true;
    }

    private static LocalTime parseExcelTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        timeStr = timeStr.trim();
        
        // Si la hora viene como "8:00" (1 dígito : 2 dígitos), le agregamos el '0' al inicio
        if (timeStr.matches("^\\d:\\d{2}.*")) {
            timeStr = "0" + timeStr;
        }
        
        // Si la hora trae segundos como "08:00:00", nos quedamos solo con los primeros 5 caracteres "08:00"
        if (timeStr.length() > 5) {
            timeStr = timeStr.substring(0, 5);
        }
        
        return LocalTime.parse(timeStr);
    }

    // =====================================================================
    // LECTURA PAGINADA DE DOCENTES (para mejorar rendimiento de carga)
    // =====================================================================
    
    /**
     * Lee docentes del Excel en lotes (para no cargar todo en memoria).
     * Devuelve el número total de filas disponibles sin datos de las mismas.
     * 
     * @param is InputStream del archivo Excel
     * @return Número total de filas de datos de docentes disponibles
     */
    public static int countTeachingRows(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;
            
            // Datos empiezan en fila 2 (índice 2)
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                
                String nombreProfesor = getCellValue(row, 5).trim();
                if (nombreProfesor.isEmpty() || nombreProfesor.equalsIgnoreCase("None")) continue;
                if (nombreProfesor.isEmpty() && getCellValue(row, 3).trim().isEmpty()) continue;
                
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Error al contar filas de Docentes en Excel: " + e.getMessage());
        }
    }
    
    /**
     * Lee docentes del Excel en lotes (paginación).
     * @param is InputStream del archivo Excel
     * @param offset Número de filas a saltar (0 para la primera)
     * @param limit Máximo número de filas a retornar
     * @return Lista de TeachingDTO para este lote
     */
    public static List<TeachingDTO> excelToTeachingBatch(InputStream is, int offset, int limit) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<TeachingDTO> docentes = new ArrayList<>();
            int skipped = 0;
            int added = 0;

            // Datos empiezan en fila 2 (índice 2), filas 0-1 son encabezados
            for (int i = 2; i <= sheet.getLastRowNum() && added < limit; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String coordinacion = getCellValue(row, 0).trim();
                String carrera = getCellValue(row, 1).trim();
                String nivel = getCellValue(row, 2).trim();
                String materia = getCellValue(row, 3).trim();
                String paralelo = getCellValue(row, 4).trim();
                String nombreProfesor = getCellValue(row, 5).trim();
                String correo = getCellValue(row, 6).trim();

                // Si no hay profesor, o dice "None", ignoramos la fila por completo
                if (nombreProfesor.isEmpty() || nombreProfesor.equalsIgnoreCase("None")) continue;
                if (nombreProfesor.isEmpty() && materia.isEmpty()) continue;

                // Saltar filas hasta llegar al offset
                if (skipped < offset) {
                    skipped++;
                    continue;
                }

                // Nombre del profesor viene en APELLIDOS NOMBRES
                String[] partes = splitNombreApellido(nombreProfesor, true);

                TeachingDTO docente = new TeachingDTO();
                docente.setCoordinacionTexto(coordinacion);
                docente.setCarreraTexto(carrera);
                docente.setNivelTexto(nivel);
                docente.setAsignaturaTexto(materia);
                docente.setParaleloTexto(paralelo);
                docente.setNombreCompleto(nombreProfesor);
                docente.setApellidos(partes[0]);
                docente.setNombres(partes[1]);
                docente.setCorreo(correo);

                docentes.add(docente);
                added++;
            }
            return docentes;
        } catch (IOException e) {
            throw new RuntimeException("Error al parsear lote de Docentes en Excel: " + e.getMessage());
        }
    }

    // =====================================================================
    // LECTURA PAGINADA DE ESTUDIANTES (para mejorar rendimiento de carga)
    // =====================================================================
    
    /**
     * Cuenta el número total de filas de estudiantes en el Excel.
     * 
     * @param is InputStream del archivo Excel
     * @param carreraTexto parámetro no usado (se incluye solo por compatibilidad)
     * @param modalidadTexto parámetro no usado (se incluye solo por compatibilidad)
     * @return Número total de filas de datos de estudiantes disponibles
     */
    public static int countStudentRows(InputStream is, String carreraTexto, String modalidadTexto) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;
            
            // Datos empiezan en fila 3 (índice 3)
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                
                String identificacion = getCellValue(row, 1).trim();
                if (identificacion.isEmpty()) continue;
                
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Error al contar filas de Estudiantes en Excel: " + e.getMessage());
        }
    }
    
    /**
     * Lee estudiantes del Excel en lotes (paginación).
     * @param is InputStream del archivo Excel
     * @param offset Número de filas a saltar (0 para la primera)
     * @param limit Máximo número de filas a retornar
     * @param carreraTexto Carrera por defecto
     * @param modalidadTexto Modalidad por defecto
     * @return Lista de StudentLoadDTO para este lote
     */
    public static List<StudentLoadDTO> excelToStudentsBatch(InputStream is, int offset, int limit, 
            String carreraTexto, String modalidadTexto) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<StudentLoadDTO> estudiantes = new ArrayList<>();
            int skipped = 0;
            int added = 0;

            // Datos empiezan en fila 3 (índice 3), filas 0-2 son encabezados
            for (int i = 3; i <= sheet.getLastRowNum() && added < limit; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String nombreCompleto = getCellValue(row, 0).trim();
                String identificacion = getCellValue(row, 1).trim();
                String correo = getCellValue(row, 2).trim();
                String telefono1 = getCellValue(row, 3).trim();
                String telefono2 = getCellValue(row, 4).trim();

                if (identificacion.isEmpty()) continue;

                // Saltar filas hasta llegar al offset
                if (skipped < offset) {
                    skipped++;
                    continue;
                }

                // Combinar teléfonos si hay dos
                String telefono = (!telefono2.isEmpty()) ? telefono1 + " / " + telefono2 : telefono1;

                // El nombre viene en formato NOMBRES APELLIDOS
                String[] partes = splitNombreApellido(nombreCompleto, false);

                StudentLoadDTO estudiante = new StudentLoadDTO();
                estudiante.setIdentificacion(identificacion);
                estudiante.setNombres(partes[0]);
                estudiante.setApellidos(partes[1]);
                estudiante.setCorreo(correo);
                estudiante.setTelefono(telefono);
                estudiante.setCarreraTexto(carreraTexto);
                estudiante.setModalidadTexto(modalidadTexto);

                estudiantes.add(estudiante);
                added++;
            }
            return estudiantes;
        } catch (IOException e) {
            throw new RuntimeException("Error al parsear lote de Estudiantes en Excel: " + e.getMessage());
        }
    }

    /**
     * Convierte un archivo Excel en una lista de mapas genéricos (clave-valor) según el tipo de carga.
     * Usado por la validación IA para analizar los datos antes de subirlos.
     *
     * @param inputStream InputStream del archivo Excel
     * @param loadType    tipo de carga: "students", "teachers", "registrations"
     * @return lista de mapas donde cada mapa representa una fila del Excel
     */
    public static List<Map<String, Object>> excelToGenericMap(InputStream inputStream, String loadType) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, Object>> rows = new ArrayList<>();

            switch (loadType.toLowerCase()) {
                case "students":
                    // Estudiantes.xls: Fila 0=encabezado institución, Fila 1=vacía, Fila 2=cabeceras, Fila 3+=datos
                    for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null || isRowEmpty(row)) continue;

                        String identificacion = getCellValue(row, 1).trim();
                        if (identificacion.isEmpty()) continue;

                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("fila", i + 1); // fila visual (1-based)
                        map.put("nombre_completo", getCellValue(row, 0).trim());
                        map.put("identificacion", identificacion);
                        map.put("correo", getCellValue(row, 2).trim());
                        map.put("telefono1", getCellValue(row, 3).trim());
                        map.put("telefono2", getCellValue(row, 4).trim());
                        rows.add(map);
                    }
                    break;

                case "teachers":
                    // Docente.xls: Fila 0=vacía, Fila 1=cabeceras, Fila 2+=datos
                    for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null || isRowEmpty(row)) continue;

                        String nombreProfesor = getCellValue(row, 5).trim();
                        if (nombreProfesor.isEmpty() || nombreProfesor.equalsIgnoreCase("None")) continue;

                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("fila", i + 1);
                        map.put("coordinacion", getCellValue(row, 0).trim());
                        map.put("carrera", getCellValue(row, 1).trim());
                        map.put("nivel", getCellValue(row, 2).trim());
                        map.put("materia", getCellValue(row, 3).trim());
                        map.put("paralelo", getCellValue(row, 4).trim());
                        map.put("profesor", nombreProfesor);
                        map.put("correo", getCellValue(row, 6).trim());
                        rows.add(map);
                    }
                    break;

                case "registrations":
                    // Matricula.xlsx: Fila 6=asignaturas, Fila 7=cabeceras, Fila 8+=datos
                    for (int i = 8; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null || isRowEmpty(row)) continue;

                        String identificador = getCellValue(row, 3).trim();
                        if (identificador.isEmpty()) continue;

                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("fila", i + 1);
                        map.put("nivel_mat", getCellValue(row, 0).trim());
                        map.put("nivel_est", getCellValue(row, 1).trim());
                        map.put("nivel_com", getCellValue(row, 2).trim());
                        map.put("identificacion", identificador);
                        map.put("apellidos", getCellValue(row, 4).trim());
                        map.put("nombres", getCellValue(row, 5).trim());
                        map.put("sexo", getCellValue(row, 6).trim());
                        map.put("email", getCellValue(row, 7).trim());
                        map.put("paralelo", getCellValue(row, 8).trim());
                        rows.add(map);
                    }
                    break;

                default:
                    // Lectura genérica: primera fila como cabeceras, resto como datos
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) return rows;

                    List<String> headers = new ArrayList<>();
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        String header = getCellValue(headerRow, c).trim();
                        headers.add(header.isEmpty() ? "col_" + c : header);
                    }

                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null || isRowEmpty(row)) continue;

                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("fila", i + 1);
                        for (int c = 0; c < headers.size(); c++) {
                            map.put(headers.get(c), getCellValue(row, c).trim());
                        }
                        rows.add(map);
                    }
                    break;
            }

            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo Excel para validación: " + e.getMessage());
        }
    }
}