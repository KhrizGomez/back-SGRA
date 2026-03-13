package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.academic.PeriodCUDDTO;
import com.CLMTZ.Backend.dto.academic.PeriodDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.academic.IPeriodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final IPeriodService periodSer;

    @GetMapping("/list-periods")
    public ResponseEntity<List<PeriodDTO>> findAll() { return ResponseEntity.ok(periodSer.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(periodSer.findById(id)); }

    @PostMapping
    public ResponseEntity<PeriodDTO> save(@RequestBody PeriodDTO dto) { return new ResponseEntity<>(periodSer.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodDTO> update(@PathVariable("id") Integer id, @RequestBody PeriodDTO dto) { return ResponseEntity.ok(periodSer.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { periodSer.deleteById(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/create-period")
    public ResponseEntity<SpResponseDTO> createPeriod(@RequestBody PeriodCUDDTO periodCUD){
        SpResponseDTO request = periodSer.createPeriod(periodCUD);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/update-period")
    public ResponseEntity<SpResponseDTO> updatePeriod(@RequestBody PeriodCUDDTO periodCUD){
        SpResponseDTO request = periodSer.updatePeriod(periodCUD);
        return ResponseEntity.ok(request);
    }
}
