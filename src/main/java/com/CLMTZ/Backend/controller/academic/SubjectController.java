package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.SubjectDTO;
import com.CLMTZ.Backend.service.academic.ISubjectService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final ISubjectService service;

    @GetMapping
    public ResponseEntity<List<SubjectDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<SubjectDTO> save(@RequestBody SubjectDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectDTO> update(@PathVariable("id") Integer id, @RequestBody SubjectDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
