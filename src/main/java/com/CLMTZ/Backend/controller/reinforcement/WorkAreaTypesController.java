package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaTypesDTO;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaTypesService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-area-types")
@RequiredArgsConstructor
public class WorkAreaTypesController {

    private final IWorkAreaTypesService service;

    @GetMapping
    public ResponseEntity<List<WorkAreaTypesDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<WorkAreaTypesDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<WorkAreaTypesDTO> save(@RequestBody WorkAreaTypesDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<WorkAreaTypesDTO> update(@PathVariable("id") Integer id, @RequestBody WorkAreaTypesDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
