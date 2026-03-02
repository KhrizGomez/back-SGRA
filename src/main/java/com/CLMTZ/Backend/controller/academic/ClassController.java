package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.ClassDTO;
import com.CLMTZ.Backend.service.academic.IClassService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/classes")
@RequiredArgsConstructor
public class ClassController {

    private final IClassService service;

    @GetMapping
    public ResponseEntity<List<ClassDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ClassDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ClassDTO> save(@RequestBody ClassDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ClassDTO> update(@PathVariable("id") Integer id, @RequestBody ClassDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
