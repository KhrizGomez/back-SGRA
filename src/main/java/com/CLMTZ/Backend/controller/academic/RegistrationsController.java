package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.RegistrationsDTO;
import com.CLMTZ.Backend.service.academic.IRegistrationsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/registrations")
@RequiredArgsConstructor
public class RegistrationsController {

    private final IRegistrationsService service;

    @GetMapping
    public ResponseEntity<List<RegistrationsDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationsDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<RegistrationsDTO> save(@RequestBody RegistrationsDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<RegistrationsDTO> update(@PathVariable("id") Integer id, @RequestBody RegistrationsDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
