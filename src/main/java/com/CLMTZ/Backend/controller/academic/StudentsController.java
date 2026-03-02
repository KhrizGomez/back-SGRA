package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.StudentsDTO;
import com.CLMTZ.Backend.service.academic.IStudentsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/students")
@RequiredArgsConstructor
public class StudentsController {

    private final IStudentsService service;

    @GetMapping
    public ResponseEntity<List<StudentsDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<StudentsDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<StudentsDTO> save(@RequestBody StudentsDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<StudentsDTO> update(@PathVariable("id") Integer id, @RequestBody StudentsDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
