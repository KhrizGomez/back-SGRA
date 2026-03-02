package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.ParallelDTO;
import com.CLMTZ.Backend.service.academic.IParallelService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/parallels")
@RequiredArgsConstructor
public class ParallelController {

    private final IParallelService service;

    @GetMapping
    public ResponseEntity<List<ParallelDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ParallelDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ParallelDTO> save(@RequestBody ParallelDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ParallelDTO> update(@PathVariable("id") Integer id, @RequestBody ParallelDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
