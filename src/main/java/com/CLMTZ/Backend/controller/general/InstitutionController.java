package com.CLMTZ.Backend.controller.general;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.general.InstitutionDTO;
import com.CLMTZ.Backend.service.general.IInstitutionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final IInstitutionService service;

    @GetMapping
    public ResponseEntity<List<InstitutionDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitutionDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<InstitutionDTO> save(@RequestBody InstitutionDTO dto) {
        return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InstitutionDTO> update(@PathVariable("id") Integer id, @RequestBody InstitutionDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
