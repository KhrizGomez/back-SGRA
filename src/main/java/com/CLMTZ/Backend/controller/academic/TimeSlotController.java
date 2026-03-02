package com.CLMTZ.Backend.controller.academic;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.academic.TimeSlotDTO;
import com.CLMTZ.Backend.service.academic.ITimeSlotService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final ITimeSlotService service;

    @GetMapping
    public ResponseEntity<List<TimeSlotDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<TimeSlotDTO> save(@RequestBody TimeSlotDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> update(@PathVariable("id") Integer id, @RequestBody TimeSlotDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
