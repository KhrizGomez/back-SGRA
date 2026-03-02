package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.AcademicAreaDTO;
import com.CLMTZ.Backend.service.academic.IAcademicAreaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/academic-areas")
@RequiredArgsConstructor
public class AcademicAreaController {

    private final IAcademicAreaService service;

    @GetMapping
    public ResponseEntity<List<AcademicAreaDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicAreaDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<AcademicAreaDTO> save(@RequestBody AcademicAreaDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<AcademicAreaDTO> update(@PathVariable("id") Integer id, @RequestBody AcademicAreaDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
