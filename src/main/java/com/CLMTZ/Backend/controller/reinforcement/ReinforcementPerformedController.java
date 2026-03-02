package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ReinforcementPerformedDTO;
import com.CLMTZ.Backend.service.reinforcement.IReinforcementPerformedService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/reinforcements-performed")
@RequiredArgsConstructor
public class ReinforcementPerformedController {

    private final IReinforcementPerformedService service;

    @GetMapping
    public ResponseEntity<List<ReinforcementPerformedDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ReinforcementPerformedDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ReinforcementPerformedDTO> save(@RequestBody ReinforcementPerformedDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ReinforcementPerformedDTO> update(@PathVariable("id") Integer id, @RequestBody ReinforcementPerformedDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
