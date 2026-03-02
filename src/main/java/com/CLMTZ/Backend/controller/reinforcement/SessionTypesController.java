package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.SessionTypesDTO;
import com.CLMTZ.Backend.service.reinforcement.ISessionTypesService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/session-types")
@RequiredArgsConstructor
public class SessionTypesController {

    private final ISessionTypesService service;

    @GetMapping
    public ResponseEntity<List<SessionTypesDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<SessionTypesDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<SessionTypesDTO> save(@RequestBody SessionTypesDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<SessionTypesDTO> update(@PathVariable("id") Integer id, @RequestBody SessionTypesDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
