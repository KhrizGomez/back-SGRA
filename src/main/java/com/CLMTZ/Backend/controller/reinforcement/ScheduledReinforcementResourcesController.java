package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementResourcesDTO;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementResourcesService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/scheduled-reinforcement-resources")
@RequiredArgsConstructor
public class ScheduledReinforcementResourcesController {

    private final IScheduledReinforcementResourcesService service;

    @GetMapping
    public ResponseEntity<List<ScheduledReinforcementResourcesDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementResourcesDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ScheduledReinforcementResourcesDTO> save(@RequestBody ScheduledReinforcementResourcesDTO dto) {
        return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReinforcementResourcesDTO> update(@PathVariable("id") Integer id, @RequestBody ScheduledReinforcementResourcesDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
