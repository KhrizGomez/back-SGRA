package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.academic.CareerLoadDTO;
import com.CLMTZ.Backend.dto.academic.ClassScheduleLoadDTO;
import com.CLMTZ.Backend.dto.academic.CoordinationDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.SubjectLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;
import com.CLMTZ.Backend.dto.ai.AIValidationRequest;
import com.CLMTZ.Backend.dto.ai.AIValidationResult;
import com.CLMTZ.Backend.service.academic.ICareerService;
import com.CLMTZ.Backend.service.academic.IClassScheduleService;
import com.CLMTZ.Backend.service.academic.ICoordinationService;
import com.CLMTZ.Backend.service.academic.IEnrollmentDetailService;
import com.CLMTZ.Backend.service.academic.ISubjectService;
import com.CLMTZ.Backend.service.ai.ExcelAIValidationService;
import com.CLMTZ.Backend.util.ExcelHelper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/coordinations")
@RequiredArgsConstructor
public class CoordinationController {

    private final ICoordinationService service;
    private final ExcelAIValidationService aiValidationService;

    @GetMapping
    public ResponseEntity<List<CoordinationDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<CoordinationDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<CoordinationDTO> save(@RequestBody CoordinationDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<CoordinationDTO> update(@PathVariable("id") Integer id, @RequestBody CoordinationDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }

    // Orden de carga: 1. Estudiantes -> 2. Matricula -> 3. Docente

    @PostMapping("/upload-students")
    public ResponseEntity<?> uploadStudents(
            @NonNull @RequestParam("file") MultipartFile file,
            @RequestParam(value = "carrera", required = false, defaultValue = "") String carreraTexto,
            @RequestParam(value = "modalidad", required = false, defaultValue = "") String modalidadTexto) {

        System.out.println("\n==============================");
        System.out.println("[UPLOAD-STUDENTS] Archivo: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("[UPLOAD-STUDENTS] Carrera (fallback): " + carreraTexto + " | Modalidad (fallback): " + modalidadTexto);
        System.out.println("==============================\n");

        if (ExcelHelper.hasExcelFormat(file)) {
            try {
                List<StudentLoadDTO> dtos = ExcelHelper.excelToStudents(file.getInputStream(), carreraTexto, modalidadTexto);
                System.out.println("[UPLOAD-STUDENTS] Filas leídas: " + dtos.size());
                List<String> reporte = service.uploadStudents(dtos);
                boolean tieneErrores = reporte.stream().anyMatch(r ->
                        r.contains(": ERROR") || r.startsWith("ADVERTENCIA") || r.startsWith("ERROR GENERAL"));
                if (tieneErrores) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(reporte);
                }
                return ResponseEntity.ok(reporte);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                        .body("No se pudo procesar el archivo: " + file.getOriginalFilename() + ". Error: " + e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Por favor, sube un archivo Excel válido (.xls o .xlsx). Archivo recibido: " + file.getOriginalFilename());
    }

    @PostMapping("/upload-registrations")
    public ResponseEntity<?> uploadRegistrations(@RequestParam("file") MultipartFile file) {

        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Error: Por favor, suba un archivo Excel válido (.xlsx)"));
        }

        try {
            List<EnrollmentDetailLoadDTO> registrationDTOs = ExcelHelper.excelToEnrollments(file.getInputStream(), file.getOriginalFilename());
            List<String> report = enrollmentDetailService.uploadEnrollmentDetails(registrationDTOs);
            boolean tieneErrores = report.stream().anyMatch(r ->
                    r.contains(": ERROR") || r.startsWith("ADVERTENCIA") || r.startsWith("ERROR GENERAL"));
            if (tieneErrores) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(report);
            }
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error al procesar el archivo de matrículas: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-teachers")
    public ResponseEntity<?> uploadTeachers(@RequestParam("file") MultipartFile file) {

        System.out.println("=== UPLOAD TEACHERS ===");
        System.out.println("Archivo recibido: " + file.getOriginalFilename());

        if (ExcelHelper.hasExcelFormat(file)) {
            try {
                List<TeachingDTO> dtos = ExcelHelper.excelToTeaching(file.getInputStream());
                System.out.println("Filas leídas del Excel: " + dtos.size());
                List<String> reporte = service.uploadTeachers(dtos);
                boolean tieneErrores = reporte.stream().anyMatch(r ->
                        r.contains(": ERROR") || r.startsWith("ADVERTENCIA") || r.startsWith("ERROR GENERAL"));
                if (tieneErrores) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(reporte);
                }
                return ResponseEntity.ok(reporte);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Error: " + e.getMessage());
            }
        }
        return ResponseEntity.badRequest()
                .body("Formato inválido. Archivo: " + file.getOriginalFilename() + ", tipo: " + file.getContentType());
    }

    private final ICareerService careerService;

    @PostMapping("/upload-careers")
    public ResponseEntity<?> uploadCareers(@RequestParam("file") MultipartFile file) {

        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Error: Por favor, suba un archivo Excel válido (.xlsx)"));
        }

        try {
            List<CareerLoadDTO> careerDTOs = ExcelHelper.excelToCareers(file.getInputStream());
            List<String> report = careerService.uploadCareers(careerDTOs);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error interno al procesar el archivo: " + e.getMessage()));
        }
    }

    private final ISubjectService subjectService;

    @PostMapping("/upload-subjects")
    public ResponseEntity<?> uploadSubjects(@RequestParam("file") MultipartFile file) {

        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Error: Por favor, suba un archivo Excel válido (.xlsx)"));
        }

        try {
            List<SubjectLoadDTO> subjectDTOs = ExcelHelper.excelToSubjects(file.getInputStream());
            List<String> report = subjectService.uploadSubjects(subjectDTOs);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error interno al procesar el archivo de asignaturas: " + e.getMessage()));
        }
    }

    private final IEnrollmentDetailService enrollmentDetailService;

    private final IClassScheduleService classScheduleService;

    @PostMapping("/upload-class-schedules")
    public ResponseEntity<?> uploadClassSchedules(@RequestParam("file") MultipartFile file) {

        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Error: Por favor, suba un archivo Excel válido (.xlsx)"));
        }

        try {
            List<ClassScheduleLoadDTO> scheduleDTOs = ExcelHelper.excelToClassSchedules(file.getInputStream());
            List<String> report = classScheduleService.uploadClassSchedules(scheduleDTOs);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error al procesar el archivo de horarios: " + e.getMessage()));
        }
    }

    // =====================================================================
    // ENDPOINT DE VALIDACIÓN IA PARA DATOS DE EXCEL
    // =====================================================================

    /**
     * Valida un archivo Excel usando IA antes de subirlo oficialmente.
     * Analiza campos vacíos, duplicados, formatos incorrectos, idioma, etc.
     *
     * @param file archivo Excel a validar
     * @param loadType tipo de carga: "students", "teachers", "registrations"
     * @return AIValidationResult con los issues encontrados y recomendación
     */
    @PostMapping("/validate-excel")
    public ResponseEntity<AIValidationResult> validateExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("loadType") String loadType) {

        System.out.println("\n==============================");
        System.out.println("[VALIDATE-EXCEL] Archivo: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("[VALIDATE-EXCEL] Tipo de carga: " + loadType);
        System.out.println("==============================\n");

        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AIValidationResult.builder()
                            .aiValidated(false)
                            .recommendedAction("REJECT")
                            .summary("El archivo no es un Excel válido (.xls o .xlsx)")
                            .build());
        }

        try {
            // Convertir Excel a lista de mapas genéricos
            List<Map<String, Object>> rows = ExcelHelper.excelToGenericMap(file.getInputStream(), loadType);

            if (rows.isEmpty()) {
                return ResponseEntity.ok(AIValidationResult.builder()
                        .aiValidated(false)
                        .recommendedAction("REJECT")
                        .summary("El archivo está vacío o no tiene datos válidos para procesar.")
                        .build());
            }

            // Construir request de validación
            AIValidationRequest request = AIValidationRequest.builder()
                    .loadType(loadType)
                    .rows(rows)
                    .build();

            // Ejecutar validación IA (con fallback a Java)
            AIValidationResult result = aiValidationService.validate(request);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AIValidationResult.builder()
                            .aiValidated(false)
                            .recommendedAction("REJECT")
                            .summary("Error al procesar el archivo: " + e.getMessage())
                            .build());
        }
    }
}
