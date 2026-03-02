package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementDetailDTO;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementDetailService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/scheduled-reinforcement-details")
@RequiredArgsConstructor
public class ScheduledReinforcementDetailController {

    private final IScheduledReinforcementDetailService service;

    @GetMapping
    public ResponseEntity<List<ScheduledReinforcementDetailDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementDetailDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ScheduledReinforcementDetailDTO> save(@RequestBody ScheduledReinforcementDetailDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementDetailDTO> update(@PathVariable("id") Integer id, @RequestBody ScheduledReinforcementDetailDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
