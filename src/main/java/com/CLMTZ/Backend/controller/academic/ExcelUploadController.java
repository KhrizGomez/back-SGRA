package com.CLMTZ.Backend.controller.academic;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.service.academic.IExcelUploadService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/excel-uploads")
@RequiredArgsConstructor
public class ExcelUploadController {

    private final IExcelUploadService excelUploadService;

    // Orden de carga: 1. Carreras -> 2. Asignaturas -> 3. Estudiantes -> 4. Matrícula -> 5. Docentes -> 6. Horarios

    @PostMapping("/upload-students")
    public ResponseEntity<?> uploadStudents(
            @NonNull @RequestParam("file") MultipartFile file,
            @RequestParam(value = "carrera", required = false, defaultValue = "") String carreraTexto,
            @RequestParam(value = "modalidad", required = false, defaultValue = "") String modalidadTexto) {

        try {
            List<String> report = excelUploadService.uploadStudents(file, carreraTexto, modalidadTexto);
            return buildUploadResponse(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(List.of("No se pudo procesar el archivo: " + file.getOriginalFilename() + ". Error: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-teachers")
    public ResponseEntity<?> uploadTeachers(@RequestParam("file") MultipartFile file) {
        try {
            List<String> report = excelUploadService.uploadTeachers(file);
            return buildUploadResponse(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(List.of("Error al procesar docentes: " + e.getMessage()));
        }
    }

    @PostMapping("/careers")
    public ResponseEntity<?> uploadCareers(@RequestParam("file") MultipartFile file) {
        try {
            List<String> report = excelUploadService.uploadCareers(file);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error interno al procesar el archivo: " + e.getMessage()));
        }
    }

    @PostMapping("/subjects")
    public ResponseEntity<?> uploadSubjects(@RequestParam("file") MultipartFile file) {
        try {
            List<String> report = excelUploadService.uploadSubjects(file);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error interno al procesar el archivo de asignaturas: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-registrations")
    public ResponseEntity<?> uploadRegistrations(@RequestParam("file") MultipartFile file) {
        try {
            List<String> report = excelUploadService.uploadRegistrations(file);
            return buildUploadResponse(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error al procesar el archivo de matrículas: " + e.getMessage()));
        }
    }

    @PostMapping("/class-schedules")
    public ResponseEntity<?> uploadClassSchedules(@RequestParam("file") MultipartFile file) {
        try {
            List<String> report = excelUploadService.uploadClassSchedules(file);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error al procesar el archivo de horarios: " + e.getMessage()));
        }
    }

    // =====================================================================
    // HELPER
    // =====================================================================

    private ResponseEntity<?> buildUploadResponse(List<String> report) {
        boolean tieneErrores = report.stream().anyMatch(r ->
                r.contains(": ERROR") || r.startsWith("ADVERTENCIA") || r.startsWith("ERROR GENERAL"));
        if (tieneErrores) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(report);
        }
        return ResponseEntity.ok(report);
    }
}
