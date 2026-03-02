package com.CLMTZ.Backend.controller.general;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.general.PreferenceDTO;
import com.CLMTZ.Backend.service.general.IPreferenceService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final IPreferenceService service;

    @GetMapping
    public ResponseEntity<List<PreferenceDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<PreferenceDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<PreferenceDTO> save(@RequestBody PreferenceDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<PreferenceDTO> update(@PathVariable("id") Integer id, @RequestBody PreferenceDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
