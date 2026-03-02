package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.ModalityDTO;
import com.CLMTZ.Backend.service.academic.IModalityService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/modalities")
@RequiredArgsConstructor
public class ModalityController {

    private final IModalityService service;

    @GetMapping
    public ResponseEntity<List<ModalityDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ModalityDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ModalityDTO> save(@RequestBody ModalityDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ModalityDTO> update(@PathVariable("id") Integer id, @RequestBody ModalityDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
