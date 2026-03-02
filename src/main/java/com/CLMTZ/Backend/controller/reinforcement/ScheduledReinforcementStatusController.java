package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementStatusDTO;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementStatusService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/scheduled-reinforcement-status")
@RequiredArgsConstructor
public class ScheduledReinforcementStatusController {

    private final IScheduledReinforcementStatusService service;

    @GetMapping
    public ResponseEntity<List<ScheduledReinforcementStatusDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementStatusDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ScheduledReinforcementStatusDTO> save(@RequestBody ScheduledReinforcementStatusDTO dto) {
        return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementStatusDTO> update(@PathVariable("id") Integer id, @RequestBody ScheduledReinforcementStatusDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
