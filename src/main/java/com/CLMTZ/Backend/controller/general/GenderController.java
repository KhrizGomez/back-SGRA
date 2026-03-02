package com.CLMTZ.Backend.controller.general;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.general.GenderDTO;
import com.CLMTZ.Backend.service.general.IGenderService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/genders")
@RequiredArgsConstructor
public class GenderController {

    private final IGenderService service;

    @GetMapping
    public ResponseEntity<List<GenderDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<GenderDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<GenderDTO> save(@RequestBody GenderDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<GenderDTO> update(@PathVariable("id") Integer id, @RequestBody GenderDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
