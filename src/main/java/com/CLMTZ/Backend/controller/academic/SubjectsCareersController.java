package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.SubjectsCareersDTO;
import com.CLMTZ.Backend.service.academic.ISubjectsCareersService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/subjects-careers")
@RequiredArgsConstructor
public class SubjectsCareersController {

    private final ISubjectsCareersService service;

    @GetMapping
    public ResponseEntity<List<SubjectsCareersDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectsCareersDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<SubjectsCareersDTO> save(@RequestBody SubjectsCareersDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectsCareersDTO> update(@PathVariable("id") Integer id, @RequestBody SubjectsCareersDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
