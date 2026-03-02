package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.PeriodDTO;
import com.CLMTZ.Backend.service.academic.IPeriodService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final IPeriodService service;

    @GetMapping
    public ResponseEntity<List<PeriodDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<PeriodDTO> save(@RequestBody PeriodDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodDTO> update(@PathVariable("id") Integer id, @RequestBody PeriodDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
