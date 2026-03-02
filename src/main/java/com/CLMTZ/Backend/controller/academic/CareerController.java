package com.CLMTZ.Backend.controller.academic;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.academic.CareerDTO;
import com.CLMTZ.Backend.dto.academic.CareerLoadDTO;
import com.CLMTZ.Backend.service.academic.ICareerService;
import com.CLMTZ.Backend.util.ExcelHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/careers")
@RequiredArgsConstructor
public class CareerController {

    private final ICareerService service;

    @GetMapping
    public ResponseEntity<List<CareerDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<CareerDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<CareerDTO> save(@RequestBody CareerDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<CareerDTO> update(@PathVariable("id") Integer id, @RequestBody CareerDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }

    @Autowired
    private ICareerService careerService; // Inyectamos el servicio de carrera
    
    @PostMapping("/upload-careers")
    public ResponseEntity<?> uploadCareers(@RequestParam("file") MultipartFile file) {
        
        // 1. Validar que el archivo sea realmente un Excel
        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Error: Por favor, suba un archivo Excel válido (.xlsx)"));
        }

        try {
            // 2. Convertir el archivo Excel a nuestra lista de DTOs usando el Helper
            List<CareerLoadDTO> careerDTOs = ExcelHelper.excelToCareers(file.getInputStream());
            
            // 3. Pasar los DTOs al servicio para que ejecute el Stored Procedure
            List<String> report = careerService.uploadCareers(careerDTOs);
            
            // 4. Devolver el reporte fila por fila al Frontend/Postman
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            // Manejo de errores generales (ej. Excel mal formateado)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Error interno al procesar el archivo: " + e.getMessage()));
        }
    }

}
