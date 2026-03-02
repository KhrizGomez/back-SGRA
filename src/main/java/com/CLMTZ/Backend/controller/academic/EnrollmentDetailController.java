package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailDTO;
import com.CLMTZ.Backend.service.academic.IEnrollmentDetailService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/enrollment-details")
@RequiredArgsConstructor
public class EnrollmentDetailController {

    private final IEnrollmentDetailService service;

    @GetMapping
    public ResponseEntity<List<EnrollmentDetailDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentDetailDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<EnrollmentDetailDTO> save(@RequestBody EnrollmentDetailDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<EnrollmentDetailDTO> update(@PathVariable("id") Integer id, @RequestBody EnrollmentDetailDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
